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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Epic("ServeRest API")
@Feature("Usuarios")
@DisplayName("POST /usuarios")
class PostUsuarioTest extends BaseTest {

    private final UsuarioClient usuarioClient = new UsuarioClient();

    @Test
    @Tag("smoke")
    @Story("Cadastro de usuario valido")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deve retornar 201 ao cadastrar usuario com dados validos")
    @DisplayName("Cadastrar usuário com dados válidos e confirmar retorno 201")
    void should_return201_when_validUser() {
        Usuario usuario = UsuarioFactory.valido();

        Response response = usuarioClient.criar(usuario);

        UsuarioAssertions.validarUsuarioCriado(response);
    }

    @Test
    @Tag("smoke")
    @Story("Cadastro com email duplicado")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 400 ao tentar cadastrar usuario com email ja existente")
    @DisplayName("Rejeitar cadastro de usuário com email já existente")
    void should_return400_when_duplicateEmail() {
        Usuario usuario = UsuarioFactory.valido();
        usuarioClient.criar(usuario);

        Usuario duplicado = UsuarioFactory.comEmailEspecifico(usuario.getEmail());

        Response response = usuarioClient.criar(duplicado);

        response.then()
                .statusCode(400)
                .body("message", equalTo("Este email já está sendo usado"));
    }

    @ParameterizedTest(name = "Rejeitar cadastro de usuário sem campo obrigatório: {0}")
    @ValueSource(strings = {"nome", "email", "password", "administrador"})
    @Story("Validacao de campos obrigatorios")
    @Severity(SeverityLevel.NORMAL)
    @Description("Deve retornar 400 ao cadastrar usuario sem campo obrigatorio")
    @DisplayName("Rejeitar cadastro de usuário sem campo obrigatório")
    void should_return400_when_missingRequiredField(String campo) {
        Usuario usuario = UsuarioFactory.semCampo(campo);

        Response response = usuarioClient.criar(usuario);

        UsuarioAssertions.validarErroDeValidacao(response, campo);
    }

    @Test
    @Story("Cadastro com corpo vazio")
    @Severity(SeverityLevel.NORMAL)
    @Description("Deve retornar 400 ao enviar corpo vazio na requisicao")
    @DisplayName("Rejeitar cadastro quando corpo da requisição estiver vazio")
    void should_return400_when_emptyBody() {
        Usuario usuario = new Usuario();

        Response response = usuarioClient.criar(usuario);

        response.then()
                .statusCode(400);
    }

    @Test
    @Tag("smoke")
    @Story("Cadastro de usuario administrador")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 201 ao cadastrar usuario com perfil administrador")
    @DisplayName("Cadastrar usuário com perfil administrador com sucesso")
    void should_return201_when_adminUser() {
        Usuario usuario = UsuarioFactory.valido();

        Response response = usuarioClient.criar(usuario);

        response.then()
                .statusCode(201)
                .body("_id", notNullValue());
    }

    @Test
    @Tag("smoke")
    @Story("Cadastro de usuario nao administrador")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 201 ao cadastrar usuario com perfil nao administrador")
    @DisplayName("Cadastrar usuário com perfil não administrador com sucesso")
    void should_return201_when_nonAdminUser() {
        Usuario usuario = UsuarioFactory.naoAdmin();

        Response response = usuarioClient.criar(usuario);

        response.then()
                .statusCode(201)
                .body("_id", notNullValue());
    }

    @Test
    @Tag("contract")
    @Story("Contrato de resposta ao cadastrar usuario")
    @Severity(SeverityLevel.NORMAL)
    @Description("Deve validar o schema JSON da resposta ao cadastrar usuario")
    @DisplayName("Validar contrato JSON Schema da resposta de cadastro de usuário")
    void should_matchJsonSchema_when_userCreated() {
        Usuario usuario = UsuarioFactory.valido();

        Response response = usuarioClient.criar(usuario);

        response.then()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath("schemas/usuario-criado-schema.json"));
    }

    @Test
    @Story("Cadastro com email em formato invalido")
    @Severity(SeverityLevel.MINOR)
    @Description("Deve retornar 400 ao cadastrar usuario com email em formato invalido")
    @DisplayName("Rejeitar cadastro quando formato do email for inválido")
    void should_return400_when_invalidEmailFormat() {
        Usuario usuario = UsuarioFactory.comEmailEspecifico("email-invalido");

        Response response = usuarioClient.criar(usuario);

        UsuarioAssertions.validarErroDeValidacao(response, "email");
    }

    @Test
    @Tag("smoke")
    @Story("Usuario recem criado aparece na listagem")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve encontrar o usuario na listagem apos o cadastro")
    @DisplayName("Confirmar que usuário recém-criado aparece na listagem")
    void should_beFoundInListing_when_justCreated() {
        Usuario usuario = UsuarioFactory.valido();
        String id = usuarioClient.criar(usuario)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");

        Response listaResponse = usuarioClient.listar();

        UsuarioAssertions.validarUsuarioNaListagem(listaResponse, id);
    }

    @Test
    @Story("Cadastro duplicado em nova tentativa")
    @Severity(SeverityLevel.NORMAL)
    @Description("Deve retornar 400 ao tentar cadastrar o mesmo email novamente")
    @DisplayName("Garantir idempotência rejeitando email duplicado em nova tentativa")
    void should_return400_when_duplicateEmailOnRetry() {
        Usuario usuario = UsuarioFactory.valido();
        usuarioClient.criar(usuario).then().statusCode(201);

        Usuario retry = UsuarioFactory.comEmailEspecifico(usuario.getEmail());

        Response response = usuarioClient.criar(retry);

        response.then()
                .statusCode(400)
                .body("message", equalTo("Este email já está sendo usado"));
    }
}
