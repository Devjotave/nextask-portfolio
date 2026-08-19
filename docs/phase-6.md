# Fase 6 — Spring Security, JWT e autorização

## 1. Objetivo

Identificar quem chama a API sem confiar em IDs enviados no corpo das requisições e aplicar a fronteira de cada workspace. O backend agora registra usuários, autentica e emite tokens JWT; todas as demais rotas usam a identidade contida no token.

## 2. Fluxo de autenticação

| Método | Caminho | Acesso | Resultado |
|---|---|---|---|
| `POST` | `/api/auth/register` | público | cria a conta e retorna `201` com token |
| `POST` | `/api/auth/login` | público | valida e-mail e senha e retorna token |
| `GET` | `/api/users/me` | autenticado | consulta a própria conta |
| `PUT` | `/api/users/me` | autenticado | atualiza a própria conta |
| `DELETE` | `/api/users/me` | autenticado | remove a própria conta quando permitido |

O registro reutiliza o serviço de usuários e o hash PBKDF2 já existente. O login usa `DaoAuthenticationProvider` e `UserDetailsService`; a API nunca compara senhas manualmente nem devolve o hash.

Uma resposta bem-sucedida de registro ou login tem este formato:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "name": "Ana Silva",
    "email": "ana@example.com"
  }
}
```

As chamadas protegidas enviam o token no cabeçalho:

```http
Authorization: Bearer eyJ...
```

## 3. Decisões do token

O JWT é assinado com HMAC SHA-256 e contém:

- `sub`: ID imutável do usuário autenticado;
- `email`: informação útil ao cliente, sem ser usada como autorização;
- `iss`: emissor configurado;
- `iat` e `exp`: emissão e expiração.

A aplicação valida assinatura, emissor e expiração em todas as rotas protegidas. Ela não cria sessão HTTP: cada requisição precisa apresentar seu token. A chave vem de `JWT_SECRET`, deve ter pelo menos 32 bytes aleatórios codificados em Base64 e não é versionada.

## 4. Identidade confiável

Os campos temporários `ownerId`, `createdById` e `authorId` foram removidos dos pedidos de criação. O usuário autenticado passa a ser automaticamente:

- proprietário ao criar um workspace;
- criador ao criar um projeto ou tarefa;
- autor ao criar um comentário.

Também foi removido o parâmetro `userId` da listagem de workspaces. `GET /api/workspaces` lista somente os workspaces do token atual, e `/api/users/me` substitui as rotas que permitiam escolher qualquer ID de usuário.

## 5. Matriz de autorização

| Operação | `MEMBER` | `OWNER` |
|---|---:|---:|
| consultar workspace e seus membros | sim | sim |
| colaborar em projetos e tarefas | sim | sim |
| criar e consultar comentários | sim | sim |
| editar ou excluir o próprio comentário | sim | sim |
| editar ou excluir comentário de outra pessoa | não | sim |
| renomear ou excluir workspace | não | sim |
| adicionar, alterar ou remover membros | não | sim |

Um usuário externo recebe `403 Forbidden`, sem poder operar sobre conteúdo do workspace. As invariantes anteriores continuam valendo: por exemplo, o workspace precisa manter pelo menos um proprietário, e um responsável de tarefa precisa pertencer à equipe.

## 6. Respostas de segurança

Os erros seguem `application/problem+json`:

- `401 Unauthorized`: credenciais incorretas, token ausente, inválido ou expirado;
- `403 Forbidden`: usuário autenticado, mas sem acesso ou papel suficiente;
- `409 Conflict`: regra de negócio violada, como remover o último proprietário.

O erro de login é propositalmente genérico — “Email or password is incorrect” — para não revelar se uma conta existe.

## 7. Configuração local

Copie `.env.example` para `.env`, troque a senha do banco e gere a chave:

```powershell
$jwtBytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
$jwtSecret = [Convert]::ToBase64String($jwtBytes)
(Get-Content .env) -replace '^JWT_SECRET=.*$', "JWT_SECRET=$jwtSecret" | Set-Content .env
```

`JWT_ISSUER` identifica a API e `JWT_ACCESS_TOKEN_TTL` aceita uma duração ISO-8601, como `PT1H`. Os testes usam uma chave exclusiva em `src/test/resources` e nunca dependem do segredo local.

## 8. Testes

```powershell
Set-Location backend
.\mvnw.cmd test
.\mvnw.cmd verify -Pintegration-test
```

Os testes rápidos validam contratos HTTP, serviços e decisões de autorização. Os testes de integração registram e autenticam usuários reais, confirmam que a senha continua armazenada somente como hash, usam JWTs assinados nas chamadas e exercitam os papéis `OWNER` e `MEMBER` contra o SQL Server.

Validação concluída no ambiente local com 35 testes rápidos e 16 testes de integração, todos aprovados sem falhas ou erros.

## 9. Referências oficiais

- [Spring Security — autenticação por usuário e senha](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/user-details-service.html)
- [Spring Security — Resource Server com JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Security — autorização de requisições](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)

## 10. Próxima fase

Documentar os contratos por OpenAPI e disponibilizar uma interface Swagger para explorar o backend autenticado.

## 11. Como explicar em uma entrevista

> Implementei autenticação stateless com Spring Security e JWT assinado por HMAC, removendo IDs de autoria controlados pelo cliente. A identidade vem do `sub` validado do token, e a autorização combina associação ao workspace com papéis `OWNER` e `MEMBER`. Separei falhas de autenticação (`401`) de autorização (`403`) e comprovei os fluxos com testes unitários e de integração no SQL Server.
