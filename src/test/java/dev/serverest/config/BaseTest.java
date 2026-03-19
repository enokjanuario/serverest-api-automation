package dev.serverest.config;

import dev.serverest.clients.CarrinhoClient;
import dev.serverest.clients.LoginClient;
import dev.serverest.clients.ProdutoClient;
import dev.serverest.clients.UsuarioClient;
import dev.serverest.factories.UsuarioFactory;
import dev.serverest.models.Usuario;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseTest.class);

    private final List<String> usuarioIds = new ArrayList<>();
    private final List<TokenAndProdutoId> produtosComToken = new ArrayList<>();
    private final List<String> tokensComCarrinho = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        RestAssuredConfig.configure();
    }

    @AfterEach
    void cleanUp() {
        CarrinhoClient carrinhoClient = new CarrinhoClient();
        for (String token : tokensComCarrinho) {
            try {
                carrinhoClient.cancelarCompra(token);
            } catch (Exception e) {
                log.warn("Failed to clean up cart: {}", e.getMessage());
            }
        }
        tokensComCarrinho.clear();

        ProdutoClient produtoClient = new ProdutoClient();
        for (TokenAndProdutoId tp : produtosComToken) {
            try {
                produtoClient.deletar(tp.token(), tp.produtoId());
            } catch (Exception e) {
                log.warn("Failed to clean up product {}: {}", tp.produtoId(), e.getMessage());
            }
        }
        produtosComToken.clear();

        UsuarioClient usuarioClient = new UsuarioClient();
        for (String id : usuarioIds) {
            try {
                usuarioClient.deletar(id);
            } catch (Exception e) {
                log.warn("Failed to clean up user {}: {}", id, e.getMessage());
            }
        }
        usuarioIds.clear();
    }

    @Step("Criar usuário administrador e obter token de autenticação")
    protected String criarUsuarioEObterToken() {
        Usuario usuario = UsuarioFactory.valido();
        UsuarioClient usuarioClient = new UsuarioClient();
        String id = usuarioClient.criar(usuario)
                .then()
                .extract()
                .path("_id");
        registrarUsuario(id);

        LoginClient loginClient = new LoginClient();
        return loginClient.obterToken(usuario.getEmail(), usuario.getPassword());
    }

    protected void registrarUsuario(String id) {
        if (id != null) {
            usuarioIds.add(id);
        }
    }

    protected void registrarProduto(String token, String produtoId) {
        if (produtoId != null) {
            produtosComToken.add(new TokenAndProdutoId(token, produtoId));
        }
    }

    protected void registrarCarrinho(String token) {
        if (token != null) {
            tokensComCarrinho.add(token);
        }
    }

    private record TokenAndProdutoId(String token, String produtoId) {}
}
