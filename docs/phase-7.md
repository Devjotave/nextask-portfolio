# Fase 7 — OpenAPI e Swagger UI

## 1. Objetivo

Transformar os contratos REST já implementados em documentação executável. A API agora publica uma descrição OpenAPI em JSON e YAML e oferece uma interface Swagger UI capaz de registrar, autenticar e chamar os endpoints protegidos com JWT.

## 2. Compatibilidade escolhida

O projeto usa Spring Boot 3.5. A matriz oficial do Springdoc associa essa linha ao Springdoc 2.8.x, por isso a dependência escolhida é `springdoc-openapi-starter-webmvc-ui` 2.8.17. A linha Springdoc 3.x é destinada ao Spring Boot 4 e não é compatível com esta base.

Referências:

- [Springdoc — primeiros passos](https://springdoc.org/getting-started.html)
- [Springdoc — matriz de compatibilidade](https://github.com/springdoc/springdoc.github.io/blob/master/src/docs/asciidoc/faq.adoc)
- [Maven Central — Springdoc WebMVC UI 2.8.17](https://central.sonatype.com/artifact/org.springdoc/springdoc-openapi-starter-webmvc-ui/2.8.17)

## 3. Endereços publicados

Com o backend em `http://localhost:8080`:

| Recurso | Endereço |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| OpenAPI YAML | `http://localhost:8080/v3/api-docs.yaml` |

Essas três rotas são públicas para que a documentação carregue antes do login. O contrato contém somente caminhos `/api/**`; endpoints internos do Actuator não são publicados.

## 4. Metadados e organização

`OpenApiConfiguration` define:

- título `NexTask API` e versão de desenvolvimento `0.1.0`;
- descrição resumida da plataforma;
- esquema HTTP Bearer chamado `bearerAuth`, com formato JWT.

Os controllers foram organizados nas tags `Authentication`, `Users`, `Workspaces`, `Projects`, `Tasks` e `Comments`. Cada operação apresenta um resumo; regras importantes, como exigência de `OWNER` e moderação de comentários, aparecem nas descrições. Respostas de criação e remoção documentam explicitamente `201 Created` e `204 No Content`.

## 5. Autenticação pela interface

1. Abra a tag **Authentication**.
2. Execute `POST /api/auth/register` para criar uma conta ou `POST /api/auth/login` para entrar.
3. Copie somente o valor de `accessToken` da resposta.
4. Clique em **Authorize** no topo da página.
5. Cole o token sem escrever `Bearer` e confirme.
6. Execute uma operação protegida, como `GET /api/users/me`.

As operações públicas de autenticação não exibem cadeado. As demais referenciam `bearerAuth`, e o Swagger UI adiciona `Authorization: Bearer <token>` às chamadas depois da autorização.

## 6. Segurança por ambiente

`OPENAPI_ENABLED=true` mantém JSON, YAML e interface ativos localmente. Em uma implantação que não deva expor documentação interativa, configure:

```text
OPENAPI_ENABLED=false
```

Isso desativa tanto `/v3/api-docs` quanto o Swagger UI sem alterar os endpoints da aplicação. A documentação nunca contém `JWT_SECRET`, senhas, tokens reais ou dados do banco.

## 7. Testes do contrato

Além dos testes existentes, o teste de contexto agora consulta a documentação sem autenticação e confirma:

- resposta JSON válida em `/v3/api-docs`;
- título e esquema `bearerAuth`;
- presença das operações públicas e protegidas;
- requisito JWT em `GET /api/users/me`;
- ausência de `/actuator/health` no contrato;
- redirecionamento correto de `/swagger-ui.html` para a interface.

Execute:

```powershell
Set-Location backend
.\mvnw.cmd clean test
.\mvnw.cmd verify -Pintegration-test
```

Resultado final da fase: 37 testes rápidos e 16 testes de integração aprovados, sem falhas, erros ou testes ignorados.

## 8. Próxima fase

Iniciar o frontend React com TypeScript, consumir os contratos documentados e construir os primeiros fluxos de autenticação e navegação.

## 9. Como explicar em uma entrevista

> Integrei Springdoc na versão compatível com Spring Boot 3.5 e tratei a documentação como um contrato testável. Organizei operações por domínio, modelei JWT como HTTP Bearer, mantive login e registro públicos e documentei as regras de autorização. Um teste consulta o JSON gerado, comprova o esquema de segurança e garante que endpoints operacionais do Actuator não vazem para o contrato público.
