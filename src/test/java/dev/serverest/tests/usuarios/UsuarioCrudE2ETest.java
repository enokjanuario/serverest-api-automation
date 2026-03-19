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
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Epic("ServeRest API")
@Feature("Usuarios")
@Story("E2E CRUD")
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UsuarioCrudE2ETest extends BaseTest {

    private final UsuarioClient usuarioClient = new UsuarioClient();

    @Test
    @Order(1)
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Executar ciclo CRUD completo: criar, buscar, atualizar, deletar")
    @Description("Fluxo completo: criar usuario, buscar por ID, atualizar, buscar novamente, deletar e confirmar exclusao")
    void should_completeCrudLifecycle_successfully() {
        Usuario usuario = criarUsuarioValido();

        String id = criarUsuarioEExtrairId(usuario);

        verificarUsuarioPorId(id, usuario);

        Usuario usuarioAtualizado = criarUsuarioAtualizado();

        atualizarUsuario(id, usuarioAtualizado);

        verificarUsuarioPorId(id, usuarioAtualizado);

        deletarUsuario(id);

        confirmarExclusao(id);
    }

    @Test
    @Order(2)
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Verificar Content-Type application/json em todos os endpoints")
    @Description("Verifica que todos os endpoints retornam Content-Type contendo application/json")
    void should_returnJsonContentType_inAllResponses() {
        Usuario usuario = UsuarioFactory.valido();

        Response criarResponse = usuarioClient.criar(usuario);
        criarResponse.then().header("Content-Type", containsString("application/json"));
        String id = criarResponse.jsonPath().getString("_id");

        Response listarResponse = usuarioClient.listar();
        listarResponse.then().header("Content-Type", containsString("application/json"));

        Response buscarResponse = usuarioClient.buscarPorId(id);
        buscarResponse.then().header("Content-Type", containsString("application/json"));

        Response atualizarResponse = usuarioClient.atualizar(id, usuario);
        atualizarResponse.then().header("Content-Type", containsString("application/json"));

        Response deletarResponse = usuarioClient.deletar(id);
        deletarResponse.then().header("Content-Type", containsString("application/json"));
    }

    @Test
    @Order(3)
    @Severity(SeverityLevel.MINOR)
    @DisplayName("Rejeitar requisição contendo campos extras no corpo")
    @Description("Envia campos adicionais no corpo da requisicao e verifica que a API rejeita com 400")
    void should_ignoreExtraFields_when_creatingUser() {
        Usuario base = UsuarioFactory.valido();

        Map<String, Object> body = new HashMap<>();
        body.put("nome", base.getNome());
        body.put("email", base.getEmail());
        body.put("password", base.getPassword());
        body.put("administrador", base.getAdministrador());
        body.put("campoExtra", "valorExtra");
        body.put("outroExtra", 12345);

        Response response = given()
                .body(body)
                .when()
                .post("/usuarios");

        response.then()
                .statusCode(400);
    }

    @Test
    @Order(4)
    @Severity(SeverityLevel.MINOR)
    @DisplayName("Garantir suporte a UTF-8 em nomes com caracteres especiais")
    @Description("Cria usuario com nome contendo acentos e caracteres especiais e verifica que o nome e armazenado corretamente")
    void should_supportUtf8_when_nameHasSpecialChars() {
        String nomeComAcentos = "Jos\u00e9 da Silva \u00c7a\u00e7ador";

        Usuario usuario = UsuarioFactory.comNome(nomeComAcentos);

        Response criarResponse = usuarioClient.criar(usuario);
        criarResponse.then()
                .statusCode(201)
                .body("_id", notNullValue());
        String id = criarResponse.jsonPath().getString("_id");

        Response buscarResponse = usuarioClient.buscarPorId(id);
        buscarResponse.then()
                .statusCode(200)
                .body("nome", equalTo(nomeComAcentos));

        usuarioClient.deletar(id);
    }

    @Step("Criar dados de usuario valido")
    private Usuario criarUsuarioValido() {
        return UsuarioFactory.valido();
    }

    @Step("Criar usuario e extrair ID")
    private String criarUsuarioEExtrairId(Usuario usuario) {
        Response response = usuarioClient.criar(usuario);

        response.then()
                .statusCode(201)
                .body("_id", notNullValue());

        return response.jsonPath().getString("_id");
    }

    @Step("Verificar dados do usuario por ID: {id}")
    private void verificarUsuarioPorId(String id, Usuario esperado) {
        Response response = usuarioClient.buscarPorId(id);

        response.then()
                .statusCode(200)
                .body("nome", equalTo(esperado.getNome()))
                .body("email", equalTo(esperado.getEmail()))
                .body("administrador", equalTo(esperado.getAdministrador()));
    }

    @Step("Criar dados de usuario atualizado")
    private Usuario criarUsuarioAtualizado() {
        return UsuarioFactory.valido();
    }

    @Step("Atualizar usuario com ID: {id}")
    private void atualizarUsuario(String id, Usuario usuario) {
        Response response = usuarioClient.atualizar(id, usuario);

        response.then()
                .statusCode(200)
                .body("message", equalTo("Registro alterado com sucesso"));
    }

    @Step("Deletar usuario com ID: {id}")
    private void deletarUsuario(String id) {
        Response response = usuarioClient.deletar(id);

        response.then()
                .statusCode(200)
                .body("message", equalTo("Registro exclu\u00eddo com sucesso"));
    }

    @Step("Confirmar que usuario com ID: {id} foi excluido")
    private void confirmarExclusao(String id) {
        Response response = usuarioClient.buscarPorId(id);

        response.then()
                .statusCode(400);
    }
}
