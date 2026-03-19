package dev.serverest.clients;

import dev.serverest.models.Usuario;

import io.qameta.allure.Step;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class UsuarioClient {

    private static final String USUARIOS = "/usuarios";
    private static final String USUARIOS_ID = "/usuarios/{_id}";

    @Step("Listar todos os usuários")
    public Response listar() {
        return given()
                .when()
                .get(USUARIOS);
    }

    @Step("Listar usuários com filtro {param} = {valor}")
    public Response listarComFiltro(String param, String valor) {
        return given()
                .queryParam(param, valor)
                .when()
                .get(USUARIOS);
    }

    @Step("Buscar usuário por ID: {id}")
    public Response buscarPorId(String id) {
        return given()
                .pathParam("_id", id)
                .when()
                .get(USUARIOS_ID);
    }

    @Step("Criar usuário")
    public Response criar(Usuario usuario) {
        return given()
                .body(usuario)
                .when()
                .post(USUARIOS);
    }

    @Step("Atualizar usuário com ID: {id}")
    public Response atualizar(String id, Usuario usuario) {
        return given()
                .pathParam("_id", id)
                .body(usuario)
                .when()
                .put(USUARIOS_ID);
    }

    @Step("Deletar usuário com ID: {id}")
    public Response deletar(String id) {
        return given()
                .pathParam("_id", id)
                .when()
                .delete(USUARIOS_ID);
    }
}
