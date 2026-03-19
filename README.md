# ServeRest API Test Automation

[![API Tests](https://github.com/enokjanuario/serverest-api-automation/actions/workflows/api-tests.yml/badge.svg)](https://github.com/enokjanuario/serverest-api-automation/actions/workflows/api-tests.yml)

Projeto de automação de testes de API para a aplicação [ServeRest](https://serverest.dev), uma API REST gratuita que simula um e-commerce. O objetivo é validar a integridade, os contratos e o comportamento dos endpoints da aplicação por meio de testes automatizados organizados em diferentes níveis da pirâmide de testes.

## Stack Utilizada

| Tecnologia | Versão | Justificativa |
|---|---|---|
| Java | 17 | Linguagem robusta com tipagem forte, amplamente adotada em automação de testes corporativos |
| JUnit 5 | 5.10.2 | Framework de testes moderno com suporte nativo a tags, parametrização e lifecycle hooks |
| REST Assured | 5.4.0 | DSL fluente para testes de API REST em Java, com validação declarativa de responses |
| Jackson | 2.17.0 | Serialização e desserialização de JSON com suporte a anotações para controle fino |
| Lombok | 1.18.30 | Redução de boilerplate em POJOs com geração automática de getters, setters e builders |
| JavaFaker | 1.0.2 | Geração de dados aleatórios realistas para isolamento entre execuções de teste |
| Allure | 2.25.0 | Framework de reports com visualização detalhada de steps, requests e responses |
| Maven | 3.8+ | Build tool com suporte a profiles para execução seletiva de suítes de teste |
| GitHub Actions | - | CI/CD nativo do GitHub com cache de dependências e deploy de reports |

## Arquitetura do Projeto

```
src/test/java/dev/serverest/
|-- clients/          # Clientes HTTP que encapsulam chamadas aos endpoints da API
|   |-- LoginClient.java
|   |-- UsuarioClient.java
|   |-- CarrinhoClient.java
|-- config/           # Configurações globais do framework e ambiente
|   |-- BaseTest.java
|   |-- Environment.java
|   |-- RestAssuredConfig.java
|-- models/           # POJOs que representam request bodies e response bodies
|   |-- Usuario.java
|   |-- LoginRequest.java
|   |-- LoginResponse.java
|-- factories/        # Factories para geração de dados de teste com JavaFaker
|   |-- UsuarioFactory.java
|-- assertions/       # Assertions customizadas com Allure Steps integrados
|   |-- UsuarioAssertions.java
```

| Pacote | Responsabilidade |
|---|---|
| `clients` | Abstrai as chamadas HTTP. Cada client corresponde a um recurso da API e expõe métodos com nomes de domínio (criar, listar, deletar). Facilita reuso e manutenção. |
| `config` | Centraliza a configuração do REST Assured (base URI, content type, filters de log e Allure). `Environment` lê a URL base de variáveis de ambiente com fallback para o valor padrão. `BaseTest` fornece setup e helpers compartilhados. |
| `models` | POJOs com Lombok e Jackson para serialização automática. `@JsonInclude(NON_NULL)` permite enviar payloads parciais para testes de validação de campos obrigatórios. |
| `factories` | Implementa o padrão Factory para gerar dados de teste. Usa JavaFaker com locale pt-BR e timestamps para garantir unicidade de emails entre execuções. |
| `assertions` | Assertions reutilizáveis anotadas com `@Step` do Allure. Separam a lógica de validação dos testes, melhorando legibilidade e rastreabilidade nos reports. |

## Pré-requisitos

| Ferramenta | Versão mínima |
|---|---|
| Java JDK | 17 |
| Maven | 3.8 |
| Git | 2.0 |

## Instalação

```bash
git clone https://github.com/enokjanuario/serverest-api-automation.git
cd serverest-api-automation
mvn install -DskipTests
```

## Execução dos Testes

| Comando | Descrição |
|---|---|
| `mvn test` | Executa todos os testes |
| `mvn test -Psmoke` | Executa apenas os testes de smoke (tag `smoke`) |
| `mvn test -Pcontract` | Executa apenas os testes de contrato (tag `contract`) |
| `mvn test -Pe2e` | Executa apenas os testes end-to-end (tag `e2e`) |
| `mvn test -Pregression` | Executa a suíte completa de regressão |
| `mvn test -Dtest=LoginTest` | Executa uma classe de teste específica |

Para apontar os testes para um ambiente diferente, defina a variável de ambiente:

```bash
export SERVEREST_URL=http://localhost:3000
mvn test
```

## Relatórios

Após a execução dos testes, gere e visualize o relatório Allure:

```bash
mvn allure:serve
```

O relatório inclui detalhes de cada step, request e response HTTP, facilitando a análise de falhas.

Na pipeline de CI, o Allure Report é publicado automaticamente no GitHub Pages após a execução da suíte de regressão na branch `main`.

## Estratégia de Testes

O projeto segue a pirâmide de testes adaptada para APIs:

| Nível | Tag | Objetivo | Exemplos |
|---|---|---|---|
| Smoke | `smoke` | Validar que os endpoints principais estão respondendo. Execução rápida para feedback imediato. | Health check, login com credenciais válidas, listagem de usuários |
| Contract | `contract` | Garantir que a estrutura das responses (campos, tipos, status codes) está conforme o contrato esperado. | Validação de JSON Schema, verificação de campos obrigatórios na response |
| E2E | `e2e` | Validar fluxos completos de negócio que envolvem múltiplos endpoints em sequência. | Cadastrar usuário, fazer login, criar carrinho, concluir compra |
| Regression | `regression` | Suíte completa com todos os níveis. Executada antes de releases para garantir estabilidade. | Todos os testes acima + cenários de boundary e negativos |

Técnicas aplicadas:

| Técnica | Aplicação |
|---|---|
| Análise de valor limite | Campos de texto com tamanho mínimo e máximo, emails no formato limite |
| Partição de equivalência | Agrupamento de cenários por comportamento esperado (usuário admin vs não-admin) |
| Teste de contrato | Validação da estrutura JSON contra schemas definidos |
| Dados dinâmicos | Uso de Factory + Faker para evitar dependência entre testes e dados fixos |
| Isolamento | Cada teste cria seus próprios dados e não depende de estado pré-existente |

## Matriz de Cobertura

| Endpoint | Smoke | Contract | E2E | Negativos |
|---|---|---|---|---|
| `POST /login` | Login válido | Schema da response | Fluxo completo de autenticação | Credenciais inválidas, campos ausentes |
| `GET /usuarios` | Listagem retorna 200 | Schema da lista | Listagem após cadastro | Filtros inválidos |
| `GET /usuarios/{id}` | Busca por ID válido | Schema do usuário | Busca após criação | ID inexistente |
| `POST /usuarios` | Criação com dados válidos | Schema de criação | Cadastro + verificação na listagem | Email duplicado, campos obrigatórios ausentes |
| `PUT /usuarios/{id}` | Atualização com dados válidos | Schema de atualização | Atualização + verificação | ID inexistente, dados inválidos |
| `DELETE /usuarios/{id}` | Deleção com ID válido | Schema de deleção | Deleção + verificação de ausência | ID inexistente, usuário com carrinho |
| `POST /carrinhos` | Criação com token válido | Schema do carrinho | Fluxo completo de compra | Token ausente, produto inexistente |

## Decisões Técnicas e Trade-offs

| Decisão | Justificativa | Trade-off |
|---|---|---|
| REST Assured sobre alternativas HTTP | DSL fluente que reduz verbosidade e melhora legibilidade dos testes | Acoplamento ao ecossistema Java; não reutilizável em projetos não-JVM |
| Client Pattern ao invés de chamadas diretas | Encapsula complexidade HTTP, permite reuso entre testes e facilita manutenção centralizada | Camada adicional de abstração; justificável pela quantidade de endpoints |
| Factory Pattern para dados | Garante unicidade e isolamento entre execuções; centraliza geração de dados | Dados aleatórios podem dificultar reprodução de falhas; mitigado com log detalhado |
| JSON Schema Validation além de assertions pontuais | Detecta mudanças estruturais na API automaticamente; complementa assertions de valor | Schemas precisam ser atualizados quando a API evolui |
| Lombok para POJOs | Elimina boilerplate de getters, setters, builders e construtores | Requer plugin no IDE; depende de annotation processing |
| JavaFaker para dados dinâmicos | Garante unicidade e isolamento entre execuções | Dados aleatórios podem dificultar reprodução de falhas; mitigado com log detalhado |
| Allure como framework de report | Reports visuais com steps, attachments e histórico integrado ao CI | Adiciona dependência ao build; geração de report consome tempo extra |
| Maven profiles para suítes | Permite execução seletiva sem alterar código; integra naturalmente com CI | Profiles duplicam configuração do surefire-plugin no pom.xml |
| Testes apontando para API pública | Elimina necessidade de infraestrutura local para rodar os testes | Dependência de disponibilidade do servidor externo; mitigado com `SERVEREST_URL` configurável |
| Assertions separadas em classes dedicadas | Reutilização de validações e integração automática com Allure Steps | Camada adicional que pode parecer over-engineering em projetos pequenos |

