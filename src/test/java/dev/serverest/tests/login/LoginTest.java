package dev.serverest.tests.login;

import dev.serverest.clients.LoginClient;
import dev.serverest.clients.UsuarioClient;
import dev.serverest.config.BaseTest;
import dev.serverest.factories.LoginFactory;
import dev.serverest.factories.UsuarioFactory;
import dev.serverest.models.LoginRequest;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@Epic("ServeRest API")
@Feature("Login")
@DisplayName("Testes de Login")
class LoginTest extends BaseTest {

    private final LoginClient loginClient = new LoginClient();
    private final UsuarioClient usuarioClient = new UsuarioClient();

    @Test
    @Tag("smoke")
    @Story("Login com credenciais validas")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifica que o login com credenciais validas retorna um token JWT")
    @DisplayName("Verificar retorno de token JWT ao autenticar com credenciais válidas")
    void should_returnToken_when_validCredentials() {
        Usuario usuario = UsuarioFactory.valido();
        String id = usuarioClient.criar(usuario).then().extract().path("_id");
        registrarUsuario(id);

        LoginRequest loginRequest = LoginFactory.comCredenciais(usuario.getEmail(), usuario.getPassword());

        Response response = loginClient.login(loginRequest);

        response.then()
                .statusCode(200)
                .body("message", equalTo("Login realizado com sucesso"))
                .body("authorization", notNullValue())
                .body("authorization", startsWith("Bearer "));
    }

    @Test
    @Tag("smoke")
    @Story("Login com email inexistente")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifica que o login com email nao cadastrado retorna 401")
    @DisplayName("Rejeitar login com email não cadastrado retornando 401")
    void should_return401_when_emailNotFound() {
        LoginRequest loginRequest = LoginFactory.comEmailInexistente();

        Response response = loginClient.login(loginRequest);

        response.then()
                .statusCode(401)
                .body("message", equalTo("Email e/ou senha inválidos"));
    }

    @Test
    @Tag("smoke")
    @Story("Login com senha incorreta")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifica que o login com senha incorreta retorna 401")
    @DisplayName("Rejeitar login com senha incorreta retornando 401")
    void should_return401_when_wrongPassword() {
        Usuario usuario = UsuarioFactory.valido();
        String id = usuarioClient.criar(usuario).then().extract().path("_id");
        registrarUsuario(id);

        LoginRequest loginRequest = LoginFactory.comSenhaErrada(usuario.getEmail());

        Response response = loginClient.login(loginRequest);

        response.then()
                .statusCode(401)
                .body("message", equalTo("Email e/ou senha inválidos"));
    }

    @Test
    @Story("Login sem campo email")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifica que o login sem o campo email retorna 400")
    @DisplayName("Rejeitar login quando campo email não for informado")
    void should_return400_when_missingEmail() {
        LoginRequest loginRequest = LoginFactory.semEmail();

        Response response = loginClient.login(loginRequest);

        response.then()
                .statusCode(400)
                .body("email", notNullValue());
    }

    @Test
    @Story("Login sem campo password")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifica que o login sem o campo password retorna 400")
    @DisplayName("Rejeitar login quando campo senha não for informado")
    void should_return400_when_missingPassword() {
        LoginRequest loginRequest = LoginFactory.semPassword();

        Response response = loginClient.login(loginRequest);

        response.then()
                .statusCode(400)
                .body("password", notNullValue());
    }

    @ParameterizedTest(name = "Rejeitar login com email inválido: {0}")
    @CsvSource({
            "email-invalido",
            "email@",
            "@dominio.com",
            "email sem arroba",
            "email@.com"
    })
    @Story("Login com formato de email invalido")
    @Severity(SeverityLevel.MINOR)
    @Description("Verifica que o login com formato de email invalido retorna 400")
    @DisplayName("Rejeitar login quando formato do email for inválido")
    void should_return400_when_invalidEmailFormat(String emailInvalido) {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(emailInvalido)
                .password("senhaValida123")
                .build();

        Response response = loginClient.login(loginRequest);

        response.then()
                .statusCode(400)
                .body("email", notNullValue());
    }

    @Test
    @Story("Login com body vazio")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verifica que o login com body vazio retorna 400")
    @DisplayName("Rejeitar login quando corpo da requisição estiver vazio")
    void should_return400_when_emptyBody() {
        LoginRequest loginRequest = LoginFactory.vazio();

        Response response = loginClient.login(loginRequest);

        response.then()
                .statusCode(400)
                .body("email", notNullValue())
                .body("password", notNullValue());
    }

    @Test
    @Tag("smoke")
    @Tag("contract")
    @Story("Validacao de schema do login com sucesso")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Valida o JSON Schema da resposta de login com sucesso")
    @DisplayName("Validar contrato JSON Schema da resposta de login bem-sucedido")
    void should_matchJsonSchema_when_loginSuccess() {
        Usuario usuario = UsuarioFactory.valido();
        String id = usuarioClient.criar(usuario).then().extract().path("_id");
        registrarUsuario(id);

        LoginRequest loginRequest = LoginFactory.comCredenciais(usuario.getEmail(), usuario.getPassword());

        Response response = loginClient.login(loginRequest);

        response.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/login-success-schema.json"));
    }

    @Test
    @Tag("contract")
    @Story("Validacao de schema do login com erro")
    @Severity(SeverityLevel.NORMAL)
    @Description("Valida o JSON Schema da resposta de erro de login")
    @DisplayName("Validar contrato JSON Schema da resposta de erro de login")
    void should_matchJsonSchema_when_loginError() {
        LoginRequest loginRequest = LoginFactory.comEmailInexistente();

        Response response = loginClient.login(loginRequest);

        response.then()
                .statusCode(401)
                .body(matchesJsonSchemaInClasspath("schemas/login-error-schema.json"));
    }

    @Test
    @Tag("security")
    @Story("Login com tentativa de SQL injection")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifica que a API rejeita tentativas de SQL injection no campo email")
    @DisplayName("Rejeitar tentativa de SQL injection no campo email")
    void should_reject_when_sqlInjectionInEmail() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("' OR 1=1 --")
                .password("qualquerSenha")
                .build();

        Response response = loginClient.login(loginRequest);

        response.then()
                .statusCode(400);
    }

    @Test
    @Tag("security")
    @Story("Login com tentativa de XSS")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verifica que a API rejeita tentativas de XSS no campo email")
    @DisplayName("Rejeitar tentativa de XSS no campo email")
    void should_reject_when_xssInEmail() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("<script>alert('xss')</script>@test.com")
                .password("qualquerSenha")
                .build();

        Response response = loginClient.login(loginRequest);

        response.then()
                .statusCode(400);
    }
}
