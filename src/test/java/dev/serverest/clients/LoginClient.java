package dev.serverest.clients;

import dev.serverest.models.LoginRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class LoginClient {

    private static final String LOGIN = "/login";

    @Step("Realizar login")
    public Response login(LoginRequest request) {
        return given()
                .body(request)
                .when()
                .post(LOGIN);
    }

    @Step("Obter token para o email: {email}")
    public String obterToken(String email, String password) {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        return login(request)
                .then()
                .statusCode(200)
                .extract()
                .path("authorization");
    }
}
