package dev.serverest.assertions;

import io.qameta.allure.Step;
import io.restassured.response.Response;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public final class ProdutoAssertions {

    private ProdutoAssertions() {
    }

    @Step("Validar que o produto foi criado com sucesso")
    public static void validarProdutoCriado(Response response) {
        response.then()
                .statusCode(201)
                .body("_id", notNullValue())
                .body("message", equalTo("Cadastro realizado com sucesso"));
    }

    @Step("Validar erro de validação no campo: {campo}")
    public static void validarErroDeValidacao(Response response, String campo) {
        response.then()
                .statusCode(400)
                .body(campo, notNullValue());
    }

    @Step("Validar que o produto não foi encontrado")
    public static void validarProdutoNaoEncontrado(Response response) {
        response.then()
                .statusCode(400)
                .body("message", equalTo("Produto não encontrado"));
    }
}
