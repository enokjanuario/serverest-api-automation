package dev.serverest.tests.usuarios;

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

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Epic("ServeRest API")
@Feature("Usuarios")
@DisplayName("GET /usuarios/{_id} - Busca por ID")
class GetUsuarioByIdTest extends BaseTest {

    private final UsuarioClient usuarioClient = new UsuarioClient();

    @Test
    @Tag("smoke")
    @Story("Buscar usuario existente por ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Valida que ao buscar um usuario existente por ID, retorna status 200 com os dados do usuario")
    @DisplayName("Buscar usuário por ID válido e verificar retorno dos dados")
    void should_return200WithUser_when_validId() {
        Usuario request = UsuarioFactory.valido();
        String id = usuarioClient.criar(request).jsonPath().getString("_id");
        registrarUsuario(id);

        Response response = usuarioClient.buscarPorId(id);

        response.then()
                .statusCode(200)
                .body("_id", equalTo(id))
                .body("nome", notNullValue());
    }

    @Test
    @Tag("smoke")
    @Story("Buscar usuario com ID inexistente")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Valida que ao buscar um usuario com ID inexistente, retorna status 400")
    @DisplayName("Rejeitar busca com ID inexistente retornando 400")
    void should_return400_when_idNotFound() {
        Response response = usuarioClient.buscarPorId("idInexistente123");

        response.then()
                .statusCode(400)
                .body("message", notNullValue());
    }

    @Test
    @Tag("contract")
    @Story("Validar contrato do usuario por ID")
    @Severity(SeverityLevel.NORMAL)
    @Description("Valida que o JSON retornado ao buscar usuario por ID segue o schema esperado")
    @DisplayName("Validar contrato JSON Schema da busca por ID")
    void should_matchJsonSchema_when_foundById() {
        Usuario request = UsuarioFactory.valido();
        String id = usuarioClient.criar(request).jsonPath().getString("_id");
        registrarUsuario(id);

        Response response = usuarioClient.buscarPorId(id);

        response.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/usuario-schema.json"));
    }

    @Test
    @Tag("smoke")
    @Story("Validar dados retornados do usuario por ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Valida que os dados retornados ao buscar por ID correspondem aos dados cadastrados")
    @DisplayName("Confirmar que dados retornados correspondem aos cadastrados")
    void should_returnCorrectData_when_foundById() {
        Usuario request = UsuarioFactory.valido();
        String id = usuarioClient.criar(request).jsonPath().getString("_id");
        registrarUsuario(id);

        Response response = usuarioClient.buscarPorId(id);

        response.then()
                .statusCode(200)
                .body("nome", equalTo(request.getNome()))
                .body("email", equalTo(request.getEmail()))
                .body("password", equalTo(request.getPassword()))
                .body("administrador", equalTo(request.getAdministrador()));
    }
}
