package dev.serverest.clients;

import dev.serverest.models.Produto;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class ProdutoClient {

    private static final String PRODUTOS = "/produtos";
    private static final String PRODUTOS_ID = "/produtos/{_id}";

    @Step("Listar todos os produtos")
    public Response listar() {
        return given()
                .when()
                .get(PRODUTOS);
    }

    @Step("Buscar produto por ID: {id}")
    public Response buscarPorId(String id) {
        return given()
                .pathParam("_id", id)
                .when()
                .get(PRODUTOS_ID);
    }

    @Step("Criar produto")
    public Response criar(String token, Produto produto) {
        return given()
                .header("Authorization", token)
                .body(produto)
                .when()
                .post(PRODUTOS);
    }

    @Step("Atualizar produto com ID: {id}")
    public Response atualizar(String token, String id, Produto produto) {
        return given()
                .header("Authorization", token)
                .pathParam("_id", id)
                .body(produto)
                .when()
                .put(PRODUTOS_ID);
    }

    @Step("Deletar produto com ID: {id}")
    public Response deletar(String token, String id) {
        return given()
                .header("Authorization", token)
                .pathParam("_id", id)
                .when()
                .delete(PRODUTOS_ID);
    }
}
