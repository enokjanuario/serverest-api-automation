package dev.serverest.tests.usuarios;

import dev.serverest.assertions.UsuarioAssertions;
import dev.serverest.clients.UsuarioClient;
import dev.serverest.config.BaseTest;
import dev.serverest.factories.UsuarioFactory;
import dev.serverest.models.Usuario;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Epic("ServeRest API")
@Feature("Usuarios")
@DisplayName("PUT /usuarios")
class PutUsuarioTest extends BaseTest {

    private final UsuarioClient usuarioClient = new UsuarioClient();

    @Test
    @Tag("smoke")
    @Story("Atualizacao de usuario existente")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 200 ao atualizar usuario existente com dados validos")
    @DisplayName("Atualizar usuário existente e verificar retorno 200")
    void should_return200_when_updatingExistingUser() {
        Usuario usuario = UsuarioFactory.valido();
        String id = usuarioClient.criar(usuario)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");
        registrarUsuario(id);

        Usuario atualizado = UsuarioFactory.valido();

        Response response = usuarioClient.atualizar(id, atualizado);

        response.then()
                .statusCode(200)
                .body("message", equalTo("Registro alterado com sucesso"));
    }

    @Test
    @Tag("smoke")
    @Story("Upsert com ID inexistente")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 201 ao fazer PUT com ID que nao existe, criando novo usuario")
    @DisplayName("Criar usuário via upsert quando ID não existir")
    void should_return201_when_idNotFound_upsert() {
        String idInexistente = UUID.randomUUID().toString();
        Usuario usuario = UsuarioFactory.valido();

        Response response = usuarioClient.atualizar(idInexistente, usuario);
        String createdId = response.jsonPath().getString("_id");
        registrarUsuario(createdId);

        response.then()
                .statusCode(201)
                .body("_id", notNullValue());
    }

    @Test
    @Tag("smoke")
    @Story("Atualizacao com email de outro usuario")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 400 ao tentar atualizar usuario com email que pertence a outro")
    @DisplayName("Rejeitar atualização quando email pertencer a outro usuário")
    void should_return400_when_emailBelongsToAnotherUser() {
        Usuario usuario1 = UsuarioFactory.valido();
        String id1 = usuarioClient.criar(usuario1).then().statusCode(201).extract().path("_id");
        registrarUsuario(id1);

        Usuario usuario2 = UsuarioFactory.valido();
        String id2 = usuarioClient.criar(usuario2)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");
        registrarUsuario(id2);

        Usuario atualizacao = UsuarioFactory.comEmailEspecifico(usuario1.getEmail());

        Response response = usuarioClient.atualizar(id2, atualizacao);

        response.then()
                .statusCode(400)
                .body("message", equalTo("Este email já está sendo usado"));
    }

    @Test
    @Story("Atualizacao sem campo obrigatorio")
    @Severity(SeverityLevel.NORMAL)
    @Description("Deve retornar 400 ao atualizar usuario sem campo obrigatorio")
    @DisplayName("Rejeitar atualização quando campo obrigatório estiver ausente")
    void should_return400_when_missingRequiredField() {
        Usuario usuario = UsuarioFactory.valido();
        String id = usuarioClient.criar(usuario)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");
        registrarUsuario(id);

        Usuario semNome = UsuarioFactory.semCampo("nome");

        Response response = usuarioClient.atualizar(id, semNome);

        UsuarioAssertions.validarErroDeValidacao(response, "nome");
    }

    @Test
    @Tag("contract")
    @Story("Contrato de resposta ao atualizar usuario")
    @Severity(SeverityLevel.NORMAL)
    @Description("Deve validar o schema JSON da resposta ao atualizar usuario")
    @DisplayName("Validar contrato JSON Schema da resposta de atualização")
    void should_matchJsonSchema_when_updated() {
        Usuario usuario = UsuarioFactory.valido();
        String id = usuarioClient.criar(usuario)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");
        registrarUsuario(id);

        Usuario atualizado = UsuarioFactory.valido();

        Response response = usuarioClient.atualizar(id, atualizado);

        response.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/mensagem-schema.json"));
    }

    @Test
    @Tag("smoke")
    @Story("Persistencia de dados apos atualizacao")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve persistir as alteracoes ao consultar o usuario apos atualizacao")
    @DisplayName("Confirmar persistência dos dados após atualização via GET")
    void should_persistChanges_when_updated() {
        Usuario usuario = UsuarioFactory.valido();
        String id = usuarioClient.criar(usuario)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");
        registrarUsuario(id);

        Usuario atualizado = UsuarioFactory.valido();
        usuarioClient.atualizar(id, atualizado).then().statusCode(200);

        Response getResponse = usuarioClient.buscarPorId(id);

        getResponse.then()
                .statusCode(200)
                .body("nome", equalTo(atualizado.getNome()))
                .body("email", equalTo(atualizado.getEmail()));
    }
}
