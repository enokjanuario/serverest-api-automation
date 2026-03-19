package dev.serverest.assertions;

import io.qameta.allure.Step;
import io.restassured.response.Response;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

public final class UsuarioAssertions {

    private UsuarioAssertions() {
    }

    @Step("Validar que o usuário foi criado com sucesso")
    public static void validarUsuarioCriado(Response response) {
        response.then()
                .statusCode(201)
                .body("_id", notNullValue());
    }

    @Step("Validar erro de validação no campo: {campo}")
    public static void validarErroDeValidacao(Response response, String campo) {
        response.then()
                .statusCode(400)
                .body(campo, notNullValue());
    }

    @Step("Validar que o usuário com id {id} aparece na listagem")
    public static void validarUsuarioNaListagem(Response listaResponse, String id) {
        listaResponse.then()
                .statusCode(200)
                .body("usuarios._id", hasItem(equalTo(id)));
    }
}
