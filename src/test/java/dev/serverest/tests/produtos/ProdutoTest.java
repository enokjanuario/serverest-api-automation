package dev.serverest.tests.produtos;

import dev.serverest.assertions.ProdutoAssertions;
import dev.serverest.clients.ProdutoClient;
import dev.serverest.config.BaseTest;
import dev.serverest.factories.ProdutoFactory;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@Epic("ServeRest API")
@Feature("Produtos")
@DisplayName("Testes de Produtos")
class ProdutoTest extends BaseTest {

    private final ProdutoClient produtoClient = new ProdutoClient();

    @Test
    @Tag("smoke")
    @Story("Listar todos os produtos")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Valida que a listagem de produtos retorna status 200")
    @DisplayName("Listar todos os produtos e verificar retorno 200")
    void should_return200_when_listingAllProducts() {
        Response response = produtoClient.listar();

        response.then()
                .statusCode(200)
                .body("quantidade", greaterThanOrEqualTo(0))
                .body("produtos", notNullValue());
    }

    @Test
    @Tag("contract")
    @Story("Validar contrato da listagem de produtos")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Valida o JSON Schema da resposta de listagem de produtos")
    @DisplayName("Validar contrato JSON Schema da listagem de produtos")
    void should_matchJsonSchema_when_listingProducts() {
        Response response = produtoClient.listar();

        response.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/produtos-list-schema.json"));
    }

    @Test
    @Tag("smoke")
    @Story("Criar produto com dados validos")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deve retornar 201 ao criar produto com dados validos")
    @DisplayName("Cadastrar produto com dados válidos e confirmar retorno 201")
    void should_return201_when_validProduct() {
        String token = criarUsuarioEObterToken();
        Produto produto = ProdutoFactory.valido();

        Response response = produtoClient.criar(token, produto);
        registrarProduto(token, response.jsonPath().getString("_id"));

        ProdutoAssertions.validarProdutoCriado(response);
    }

    @Test
    @Tag("contract")
    @Story("Validar contrato de criacao de produto")
    @Severity(SeverityLevel.NORMAL)
    @Description("Valida o JSON Schema da resposta de criacao de produto")
    @DisplayName("Validar contrato JSON Schema da resposta de criação de produto")
    void should_matchJsonSchema_when_productCreated() {
        String token = criarUsuarioEObterToken();
        Produto produto = ProdutoFactory.valido();

        Response response = produtoClient.criar(token, produto);
        registrarProduto(token, response.jsonPath().getString("_id"));

        response.then()
                .statusCode(201)
                .body(matchesJsonSchemaInClasspath("schemas/produto-criado-schema.json"));
    }

    @Test
    @Tag("smoke")
    @Story("Buscar produto por ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 200 ao buscar produto existente por ID")
    @DisplayName("Buscar produto por ID válido e verificar dados retornados")
    void should_return200_when_validProductId() {
        String token = criarUsuarioEObterToken();
        Produto produto = ProdutoFactory.valido();
        String id = produtoClient.criar(token, produto)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");
        registrarProduto(token, id);

        Response response = produtoClient.buscarPorId(id);

        response.then()
                .statusCode(200)
                .body("nome", equalTo(produto.getNome()))
                .body("preco", equalTo(produto.getPreco()))
                .body("_id", equalTo(id));
    }

    @Test
    @Story("Buscar produto com ID inexistente")
    @Severity(SeverityLevel.NORMAL)
    @Description("Deve retornar 400 ao buscar produto com ID que nao existe")
    @DisplayName("Rejeitar busca de produto com ID inexistente")
    void should_return400_when_productIdNotFound() {
        Response response = produtoClient.buscarPorId("idInexistente123");

        ProdutoAssertions.validarProdutoNaoEncontrado(response);
    }

    @Test
    @Tag("contract")
    @Story("Validar contrato de produto por ID")
    @Severity(SeverityLevel.NORMAL)
    @Description("Valida o JSON Schema ao buscar produto por ID")
    @DisplayName("Validar contrato JSON Schema da busca de produto por ID")
    void should_matchJsonSchema_when_foundById() {
        String token = criarUsuarioEObterToken();
        Produto produto = ProdutoFactory.valido();
        String id = produtoClient.criar(token, produto)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");
        registrarProduto(token, id);

        Response response = produtoClient.buscarPorId(id);

        response.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/produto-schema.json"));
    }

    @Test
    @Tag("smoke")
    @Story("Atualizar produto existente")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 200 ao atualizar produto existente com dados validos")
    @DisplayName("Atualizar produto existente e verificar retorno 200")
    void should_return200_when_updatingExistingProduct() {
        String token = criarUsuarioEObterToken();
        Produto produto = ProdutoFactory.valido();
        String id = produtoClient.criar(token, produto)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");
        registrarProduto(token, id);

        Produto atualizado = ProdutoFactory.valido();

        Response response = produtoClient.atualizar(token, id, atualizado);

        response.then()
                .statusCode(200)
                .body("message", equalTo("Registro alterado com sucesso"));
    }

    @Test
    @Tag("smoke")
    @Story("Deletar produto existente")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 200 ao deletar produto existente sem vinculo a carrinho")
    @DisplayName("Excluir produto existente e verificar exclusão com retorno 200")
    void should_return200_when_deletingExistingProduct() {
        String token = criarUsuarioEObterToken();
        Produto produto = ProdutoFactory.valido();
        String id = produtoClient.criar(token, produto)
                .then()
                .statusCode(201)
                .extract()
                .path("_id");

        Response response = produtoClient.deletar(token, id);

        response.then()
                .statusCode(200)
                .body("message", equalTo("Registro excluído com sucesso"));
    }

    @Test
    @Story("Criar produto sem autenticacao")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 401 ao criar produto sem token de autenticacao")
    @DisplayName("Rejeitar criação de produto sem token de autenticação")
    void should_return401_when_noAuthToken() {
        Produto produto = ProdutoFactory.valido();

        Response response = produtoClient.criar("", produto);

        response.then()
                .statusCode(401)
                .body("message", equalTo("Token de acesso ausente, inválido, expirado ou usuário do token não existe mais"));
    }

    @Test
    @Tag("security")
    @Story("Criar produto com token invalido")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 401 ao criar produto com token forjado")
    @DisplayName("Rejeitar criação de produto com token inválido/forjado")
    void should_return401_when_invalidToken() {
        Produto produto = ProdutoFactory.valido();

        Response response = produtoClient.criar("Bearer token.invalido.forjado", produto);

        response.then()
                .statusCode(401)
                .body("message", equalTo("Token de acesso ausente, inválido, expirado ou usuário do token não existe mais"));
    }

    @ParameterizedTest(name = "Rejeitar criação de produto sem campo obrigatório: {0}")
    @ValueSource(strings = {"nome", "preco", "descricao", "quantidade"})
    @Story("Validacao de campos obrigatorios do produto")
    @Severity(SeverityLevel.NORMAL)
    @Description("Deve retornar 400 ao criar produto sem campo obrigatorio")
    @DisplayName("Rejeitar criação de produto sem campo obrigatório")
    void should_return400_when_missingRequiredField(String campo) {
        String token = criarUsuarioEObterToken();
        Produto produto = ProdutoFactory.semCampo(campo);

        Response response = produtoClient.criar(token, produto);

        ProdutoAssertions.validarErroDeValidacao(response, campo);
    }

    @ParameterizedTest(name = "Rejeitar criação de produto com valor de preço inválido: {0}")
    @CsvSource({
            "0",
            "-1",
            "-100"
    })
    @Story("Validacao de valores invalidos de preco")
    @Severity(SeverityLevel.NORMAL)
    @Description("Deve retornar 400 ao criar produto com preco invalido")
    @DisplayName("Rejeitar criação de produto com preço inválido")
    void should_return400_when_invalidPrice(int preco) {
        String token = criarUsuarioEObterToken();
        Produto produto = ProdutoFactory.valido();
        produto.setPreco(preco);

        Response response = produtoClient.criar(token, produto);

        response.then()
                .statusCode(400);
    }

    @Test
    @Tag("smoke")
    @Story("Criar produto com nome duplicado")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deve retornar 400 ao criar produto com nome ja existente")
    @DisplayName("Rejeitar cadastro de produto com nome duplicado")
    void should_return400_when_duplicateProductName() {
        String token = criarUsuarioEObterToken();
        Produto produto = ProdutoFactory.valido();
        String id = produtoClient.criar(token, produto).then().statusCode(201).extract().path("_id");
        registrarProduto(token, id);

        Produto duplicado = ProdutoFactory.valido();
        duplicado.setNome(produto.getNome());

        Response response = produtoClient.criar(token, duplicado);

        response.then()
                .statusCode(400)
                .body("message", equalTo("Já existe produto com esse nome"));
    }
}
