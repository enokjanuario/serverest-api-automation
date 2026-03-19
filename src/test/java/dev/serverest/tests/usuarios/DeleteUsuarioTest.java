package dev.serverest.tests.usuarios;

import dev.serverest.assertions.UsuarioAssertions;
import dev.serverest.clients.CarrinhoClient;
import dev.serverest.clients.LoginClient;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;

@Epic("ServeRest API")
@Feature("Usuarios")
@DisplayName("DELETE /usuarios")
class DeleteUsuarioTest extends BaseTest {

    private final UsuarioClient usuarioClient = new UsuarioClient();
    private final LoginClient loginClient = new LoginClient();
    private final CarrinhoClient carrinhoClient = new CarrinhoClient();

    @Test
    @Tag("smoke")
    @Story("Exclusao de usuario existente")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 200 ao deletar usuario existente")
    @DisplayName("Excluir usuário existente e verificar retorno 200")
    void should_return200_when_deletingExistingUser() {
        Usuario usuario = UsuarioFactory.valido();
        String id = usuarioClient.criar(usuario)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");

        Response response = usuarioClient.deletar(id);

        response.then()
                .statusCode(200)
                .body("message", equalTo("Registro excluído com sucesso"));
    }

    @Test
    @Story("Exclusao com ID inexistente")
    @Severity(SeverityLevel.NORMAL)
    @Description("Deve retornar 200 ao tentar deletar usuario com ID inexistente")
    @DisplayName("Verificar resposta idempotente ao excluir ID inexistente")
    void should_return200_when_idNotFound() {
        String idInexistente = UUID.randomUUID().toString();

        Response response = usuarioClient.deletar(idInexistente);

        response.then()
                .statusCode(200)
                .body("message", equalTo("Nenhum registro excluído"));
    }

    @Test
    @Tag("smoke")
    @Story("Exclusao de usuario com carrinho")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 400 ao tentar deletar usuario que possui carrinho")
    @DisplayName("Bloquear exclusão de usuário que possui carrinho ativo")
    void should_return400_when_userHasCart() {
        Usuario usuario = UsuarioFactory.valido();
        String id = usuarioClient.criar(usuario)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");

        String token = loginClient.obterToken(usuario.getEmail(), usuario.getPassword());

        String idProduto = given()
                .header("Authorization", token)
                .body(Map.of(
                        "nome", "Produto Test " + System.nanoTime(),
                        "preco", 100,
                        "descricao", "Test",
                        "quantidade", 10
                ))
                .when()
                .post("/produtos")
                .then()
                .statusCode(201)
                .extract()
                .path("_id");

        Map<String, Object> carrinho = Map.of(
                "produtos", List.of(Map.of("idProduto", idProduto, "quantidade", 1))
        );
        carrinhoClient.criar(token, carrinho).then().statusCode(201);

        Response response = usuarioClient.deletar(id);

        response.then()
                .statusCode(400)
                .body("message", equalTo("Não é permitido excluir usuário com carrinho cadastrado"));
    }

    @Test
    @Tag("smoke")
    @Story("Usuario excluido nao aparece na listagem")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve verificar que o usuario nao aparece na listagem apos exclusao")
    @DisplayName("Confirmar ausência do usuário na listagem após exclusão")
    void should_notBeFoundInListing_when_deleted() {
        Usuario usuario = UsuarioFactory.valido();
        String id = usuarioClient.criar(usuario)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");

        usuarioClient.deletar(id).then().statusCode(200);

        Response listaResponse = usuarioClient.listar();

        listaResponse.then()
                .statusCode(200)
                .body("usuarios._id", not(hasItem(equalTo(id))));
    }

    @Test
    @Tag("contract")
    @Story("Contrato de resposta ao deletar usuario")
    @Severity(SeverityLevel.NORMAL)
    @Description("Deve validar o schema JSON da resposta ao deletar usuario")
    @DisplayName("Validar contrato JSON Schema da resposta de exclusão")
    void should_matchJsonSchema_when_deleted() {
        Usuario usuario = UsuarioFactory.valido();
        String id = usuarioClient.criar(usuario)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");

        Response response = usuarioClient.deletar(id);

        response.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/mensagem-schema.json"));
    }
}
