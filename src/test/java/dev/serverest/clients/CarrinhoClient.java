package dev.serverest.clients;

import io.qameta.allure.Step;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class CarrinhoClient {

    private static final String CARRINHOS = "/carrinhos";
    private static final String CARRINHOS_CONCLUIR = "/carrinhos/concluir-compra";
    private static final String CARRINHOS_CANCELAR = "/carrinhos/cancelar-compra";

    @Step("Criar carrinho")
    public Response criar(String token, Object carrinho) {
        return given()
                .header("Authorization", token)
                .body(carrinho)
                .when()
                .post(CARRINHOS);
    }

    @Step("Listar todos os carrinhos")
    public Response listar() {
        return given()
                .when()
                .get(CARRINHOS);
    }

    @Step("Buscar carrinho por ID: {id}")
    public Response buscarPorId(String id) {
        return given()
                .pathParam("_id", id)
                .when()
                .get(CARRINHOS + "/{_id}");
    }

    @Step("Concluir compra do carrinho")
    public Response concluirCompra(String token) {
        return given()
                .header("Authorization", token)
                .when()
                .delete(CARRINHOS_CONCLUIR);
    }

    @Step("Cancelar compra do carrinho")
    public Response cancelarCompra(String token) {
        return given()
                .header("Authorization", token)
                .when()
                .delete(CARRINHOS_CANCELAR);
    }
}
