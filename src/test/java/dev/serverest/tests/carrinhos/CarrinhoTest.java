package dev.serverest.tests.carrinhos;

import dev.serverest.assertions.CarrinhoAssertions;
import dev.serverest.clients.CarrinhoClient;
import dev.serverest.clients.ProdutoClient;
import dev.serverest.config.BaseTest;
import dev.serverest.factories.CarrinhoFactory;
import dev.serverest.factories.ProdutoFactory;
import dev.serverest.models.Carrinho;
import dev.serverest.models.Produto;

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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@Epic("ServeRest API")
@Feature("Carrinhos")
@DisplayName("Testes de Carrinhos")
class CarrinhoTest extends BaseTest {

    private final CarrinhoClient carrinhoClient = new CarrinhoClient();
    private final ProdutoClient produtoClient = new ProdutoClient();

    @Test
    @Tag("smoke")
    @Story("Listar todos os carrinhos")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Valida que a listagem de carrinhos retorna status 200")
    @DisplayName("Listar todos os carrinhos e verificar retorno 200")
    void should_return200_when_listingAllCarts() {
        Response response = carrinhoClient.listar();

        response.then()
                .statusCode(200)
                .body("quantidade", greaterThanOrEqualTo(0))
                .body("carrinhos", notNullValue());
    }

    @Test
    @Tag("contract")
    @Story("Validar contrato da listagem de carrinhos")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Valida o JSON Schema da resposta de listagem de carrinhos")
    @DisplayName("Validar contrato JSON Schema da listagem de carrinhos")
    void should_matchJsonSchema_when_listingCarts() {
        Response response = carrinhoClient.listar();

        response.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/carrinhos-list-schema.json"));
    }

    @Test
    @Tag("smoke")
    @Story("Criar carrinho com produto valido")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deve retornar 201 ao criar carrinho com produto existente")
    @DisplayName("Cadastrar carrinho com produto válido e confirmar retorno 201")
    void should_return201_when_validCart() {
        String token = criarUsuarioEObterToken();
        String idProduto = criarProdutoEExtrairId(token);

        Carrinho carrinho = CarrinhoFactory.comProduto(idProduto, 1);

        Response response = carrinhoClient.criar(token, carrinho);
        registrarCarrinho(token);

        CarrinhoAssertions.validarCarrinhoCriado(response);
    }

    @Test
    @Story("Criar carrinho sem autenticacao")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 401 ao criar carrinho sem token")
    @DisplayName("Rejeitar criação de carrinho sem token de autenticação")
    void should_return401_when_noAuthToken() {
        Carrinho carrinho = CarrinhoFactory.comProduto("qualquerId", 1);

        Response response = carrinhoClient.criar("", carrinho);

        CarrinhoAssertions.validarTokenAusenteOuInvalido(response);
    }

    @Test
    @Tag("security")
    @Story("Criar carrinho com token invalido")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 401 ao criar carrinho com token forjado")
    @DisplayName("Rejeitar criação de carrinho com token inválido/forjado")
    void should_return401_when_invalidToken() {
        Carrinho carrinho = CarrinhoFactory.comProduto("qualquerId", 1);

        Response response = carrinhoClient.criar("Bearer token.invalido.forjado", carrinho);

        CarrinhoAssertions.validarTokenAusenteOuInvalido(response);
    }

    @Test
    @Tag("smoke")
    @Story("Criar segundo carrinho para o mesmo usuario")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 400 ao criar segundo carrinho para usuario que ja possui um")
    @DisplayName("Bloquear criação de segundo carrinho para o mesmo usuário")
    void should_return400_when_userAlreadyHasCart() {
        String token = criarUsuarioEObterToken();
        String idProduto1 = criarProdutoEExtrairId(token);
        String idProduto2 = criarProdutoEExtrairId(token);

        Carrinho carrinho1 = CarrinhoFactory.comProduto(idProduto1, 1);
        carrinhoClient.criar(token, carrinho1).then().statusCode(201);
        registrarCarrinho(token);

        Carrinho carrinho2 = CarrinhoFactory.comProduto(idProduto2, 1);

        Response response = carrinhoClient.criar(token, carrinho2);

        response.then()
                .statusCode(400)
                .body("message", equalTo("Não é permitido ter mais de 1 carrinho"));
    }

    @Test
    @Story("Criar carrinho com produto inexistente")
    @Severity(SeverityLevel.NORMAL)
    @Description("Deve retornar 400 ao criar carrinho com idProduto que nao existe")
    @DisplayName("Rejeitar carrinho quando produto informado não existir")
    void should_return400_when_productNotFound() {
        String token = criarUsuarioEObterToken();
        Carrinho carrinho = CarrinhoFactory.comProduto("produtoInexistente123", 1);

        Response response = carrinhoClient.criar(token, carrinho);

        response.then()
                .statusCode(400)
                .body("message", equalTo("Produto não encontrado"));
    }

    @Test
    @Tag("smoke")
    @Story("Concluir compra do carrinho")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 200 ao concluir compra do carrinho existente")
    @DisplayName("Concluir compra do carrinho e verificar exclusão com sucesso")
    void should_return200_when_completingPurchase() {
        String token = criarUsuarioEObterToken();
        String idProduto = criarProdutoEExtrairId(token);

        Carrinho carrinho = CarrinhoFactory.comProduto(idProduto, 1);
        carrinhoClient.criar(token, carrinho).then().statusCode(201);

        Response response = carrinhoClient.concluirCompra(token);

        CarrinhoAssertions.validarCompraConcluida(response);
    }

    @Test
    @Tag("smoke")
    @Story("Cancelar compra do carrinho")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 200 ao cancelar compra do carrinho existente")
    @DisplayName("Cancelar compra do carrinho e verificar reabastecimento do estoque")
    void should_return200_when_cancellingPurchase() {
        String token = criarUsuarioEObterToken();
        String idProduto = criarProdutoEExtrairId(token);

        Carrinho carrinho = CarrinhoFactory.comProduto(idProduto, 1);
        carrinhoClient.criar(token, carrinho).then().statusCode(201);

        Response response = carrinhoClient.cancelarCompra(token);

        CarrinhoAssertions.validarCompraCancelada(response);
    }

    private String criarProdutoEExtrairId(String token) {
        Produto produto = ProdutoFactory.valido();
        String id = produtoClient.criar(token, produto)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");
        registrarProduto(token, id);
        return id;
    }
}
