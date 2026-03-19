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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Epic("ServeRest API")
@Feature("Usuarios")
@DisplayName("GET /usuarios - Listagem de usuários")
class GetUsuariosTest extends BaseTest {

    private UsuarioClient usuarioClient;

    @BeforeEach
    void init() {
        usuarioClient = new UsuarioClient();
    }

    @Test
    @Tag("smoke")
    @Story("Listar todos os usuarios")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Valida que a listagem de usuarios retorna status 200 e um array de usuarios")
    @DisplayName("Listar todos os usuários e verificar retorno 200 com array")
    void should_return200WithUserList_when_listingAllUsers() {
        Response response = usuarioClient.listar();

        response.then()
                .statusCode(200)
                .body("quantidade", greaterThanOrEqualTo(1))
                .body("usuarios", notNullValue());
    }

    @Test
    @Story("Filtrar usuarios por nome")
    @Severity(SeverityLevel.NORMAL)
    @Description("Valida que ao passar o query param nome, apenas usuarios com aquele nome sao retornados")
    @DisplayName("Filtrar usuários por nome e verificar resultados correspondentes")
    void should_filterByNome_when_queryParamProvided() {
        Usuario request = UsuarioFactory.valido();
        usuarioClient.criar(request);

        Response response = usuarioClient.listarComFiltro("nome", request.getNome());

        response.then()
                .statusCode(200)
                .body("usuarios.nome", everyItem(equalTo(request.getNome())));
    }

    @Test
    @Story("Filtrar usuarios por email")
    @Severity(SeverityLevel.NORMAL)
    @Description("Valida que ao passar o query param email, apenas usuarios com aquele email sao retornados")
    @DisplayName("Filtrar usuários por email e verificar resultados correspondentes")
    void should_filterByEmail_when_queryParamProvided() {
        Usuario request = UsuarioFactory.valido();
        usuarioClient.criar(request);

        Response response = usuarioClient.listarComFiltro("email", request.getEmail());

        response.then()
                .statusCode(200)
                .body("usuarios.email", everyItem(equalTo(request.getEmail())));
    }

    @Test
    @Story("Filtrar usuarios por campo administrador")
    @Severity(SeverityLevel.MINOR)
    @Description("Valida que ao passar o query param administrador, apenas usuarios com aquele valor sao retornados")
    @DisplayName("Filtrar usuários por perfil administrador e verificar resultados")
    void should_filterByAdmin_when_queryParamProvided() {
        Response response = usuarioClient.listarComFiltro("administrador", "true");

        response.then()
                .statusCode(200)
                .body("usuarios.administrador", everyItem(equalTo("true")));
    }

    @Test
    @Tag("contract")
    @Story("Validar contrato da listagem de usuarios")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Valida que o JSON retornado pela listagem de usuarios segue o schema esperado")
    @DisplayName("Validar contrato JSON Schema da listagem de usuários")
    void should_matchJsonSchema_when_listingUsers() {
        Response response = usuarioClient.listar();

        response.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/usuarios-list-schema.json"));
    }

    @Test
    @Story("Filtrar usuarios sem resultado")
    @Severity(SeverityLevel.MINOR)
    @Description("Valida que ao filtrar com valor inexistente, a lista retornada e vazia")
    @DisplayName("Retornar lista vazia ao filtrar por usuário inexistente")
    void should_returnEmptyList_when_filterMatchesNoUser() {
        Response response = usuarioClient.listarComFiltro("nome", "UsuarioInexistente_999999");

        response.then()
                .statusCode(200)
                .body("quantidade", equalTo(0))
                .body("usuarios", is(empty()));
    }

    @Test
    @Story("Validar quantidade na listagem")
    @Severity(SeverityLevel.NORMAL)
    @Description("Valida que o campo quantidade corresponde ao tamanho do array de usuarios")
    @DisplayName("Verificar que campo quantidade corresponde ao tamanho do array")
    void should_returnQuantidade_matching_arrayLength() {
        Response response = usuarioClient.listar();

        int quantidade = response.jsonPath().getInt("quantidade");
        int arraySize = response.jsonPath().getList("usuarios").size();

        org.junit.jupiter.api.Assertions.assertEquals(quantidade, arraySize);
    }
}
