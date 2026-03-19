package dev.serverest.assertions;

import io.qameta.allure.Step;
import io.restassured.response.Response;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public final class CarrinhoAssertions {

    private CarrinhoAssertions() {
    }

    @Step("Validar que o carrinho foi criado com sucesso")
    public static void validarCarrinhoCriado(Response response) {
        response.then()
                .statusCode(201)
                .body("message", equalTo("Cadastro realizado com sucesso"))
                .body("_id", notNullValue());
    }

    @Step("Validar que a compra foi concluída com sucesso")
    public static void validarCompraConcluida(Response response) {
        response.then()
                .statusCode(200)
                .body("message", equalTo("Registro excluído com sucesso"));
    }

    @Step("Validar que a compra foi cancelada e estoque reabastecido")
    public static void validarCompraCancelada(Response response) {
        response.then()
                .statusCode(200)
                .body("message", equalTo("Registro excluído com sucesso. Estoque dos produtos reabastecido"));
    }

    @Step("Validar token ausente ou inválido")
    public static void validarTokenAusenteOuInvalido(Response response) {
        response.then()
                .statusCode(401)
                .body("message", equalTo("Token de acesso ausente, inválido, expirado ou usuário do token não existe mais"));
    }
}
