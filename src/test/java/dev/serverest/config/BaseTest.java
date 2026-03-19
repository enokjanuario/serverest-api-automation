package dev.serverest.config;

import dev.serverest.clients.LoginClient;
import dev.serverest.clients.UsuarioClient;
import dev.serverest.factories.UsuarioFactory;
import dev.serverest.models.Usuario;
import io.qameta.allure.Step;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseTest {

    @BeforeAll
    static void setUp() {
        RestAssuredConfig.configure();
    }

    @Step("Criar usuário administrador e obter token de autenticação")
    protected String criarUsuarioEObterToken() {
        Usuario usuario = UsuarioFactory.valido();
        UsuarioClient usuarioClient = new UsuarioClient();
        usuarioClient.criar(usuario);

        LoginClient loginClient = new LoginClient();
        return loginClient.obterToken(usuario.getEmail(), usuario.getPassword());
    }
}
