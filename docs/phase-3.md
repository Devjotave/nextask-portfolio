# Fase 3 — API de usuários

## 1. Objetivo

Disponibilizar os primeiros casos de uso HTTP do NexTask sobre o modelo criado na Fase 2. A API permite cadastrar, paginar, consultar, atualizar e remover usuários sem expor a entidade JPA nem qualquer dado de senha.

Esta fase ainda não implementa login nem autorização. Os endpoints permanecem abertos apenas para desenvolvimento local até a fase de Spring Security e JWT.

## 2. Endpoints

| Método | Caminho | Resultado |
|---|---|---|
| `POST` | `/api/users` | Cadastra um usuário e retorna `201 Created` |
| `GET` | `/api/users?page=0&size=20` | Lista usuários com paginação, ordenados por `id` |
| `GET` | `/api/users/{id}` | Consulta um usuário ou retorna `404 Not Found` |
| `PUT` | `/api/users/{id}` | Atualiza nome e e-mail |
| `DELETE` | `/api/users/{id}` | Remove o usuário e retorna `204 No Content` |

A listagem aceita páginas a partir de zero e tamanhos entre 1 e 100. A senha não faz parte da atualização geral; troca de senha exigirá um caso de uso próprio quando houver autenticação.

## 3. Organização do código

O módulo `com.nextask.user` reúne a funcionalidade por domínio:

- `UserRepository`: abstração Spring Data JPA e consultas de unicidade;
- `UserService`: transações e casos de uso;
- `UserController`: contrato HTTP;
- `api`: DTOs de entrada e saída;
- `error`: exceções específicas do módulo.

Os elementos compartilhados entre módulos, `PageResponse` e `ApiExceptionHandler`, passaram a morar em `com.nextask.api` na Fase 4.

O controller nunca devolve `User` diretamente. `UserResponse` contém somente `id`, `name`, `email`, `createdAt` e `updatedAt`, impedindo que `passwordHash` seja serializado por acidente.

## 4. Cadastro

Exemplo de requisição:

```http
POST /api/users
Content-Type: application/json

{
  "name": "Ada Lovelace",
  "email": "ada@example.com",
  "password": "a-secure-password"
}
```

Exemplo de resposta:

```http
HTTP/1.1 201 Created
Location: /api/users/1
Content-Type: application/json

{
  "id": 1,
  "name": "Ada Lovelace",
  "email": "ada@example.com",
  "createdAt": "2026-08-18T23:00:00Z",
  "updatedAt": "2026-08-18T23:00:00Z"
}
```

Nome, e-mail e senha são validados antes do serviço. O e-mail é normalizado para minúsculas, verificado pela aplicação e protegido definitivamente pela constraint única do SQL Server. A captura de `DataIntegrityViolationException` cobre a condição de corrida entre a verificação e a gravação.

## 5. Senha

A senha existe em texto puro somente no DTO recebido. O serviço aplica PBKDF2 com salt aleatório e parâmetros versionados do Spring Security antes de construir a entidade.

O valor persistido começa com `{pbkdf2@SpringSecurity_v5_8}`. Esse identificador permite reconhecer o algoritmo e migrar hashes no futuro sem manter senha reversível. A coluna de 255 caracteres criada na Fase 2 comporta o resultado.

Foi adicionado somente `spring-security-crypto`; autenticação, filtros, sessão e JWT continuam fora do escopo desta fase.

## 6. Erros HTTP

Os erros seguem `ProblemDetail`, formato baseado no padrão Problem Details for HTTP APIs:

- `400 Bad Request`: campos inválidos;
- `404 Not Found`: usuário inexistente;
- `409 Conflict`: e-mail já utilizado.

Na validação, a propriedade `errors` relaciona cada campo à sua mensagem. Detalhes internos do banco e o hash da senha nunca são incluídos.

## 7. Testes

Testes rápidos, sem SQL Server:

```powershell
Set-Location backend
.\mvnw.cmd test
```

Eles cobrem regras do serviço, normalização, hash, conflitos, validação dos DTOs, status HTTP e ausência de dados de senha nas respostas.

Testes completos com SQL Server:

```powershell
.\mvnw.cmd verify -Pintegration-test
```

`UserApiIT` percorre a API com `MockMvc` e o banco real. Ele confirma cadastro, leitura, paginação, atualização, exclusão, conflito de e-mail e correspondência do hash usando `PasswordEncoder.matches`.

## 8. Teste manual opcional

Com o backend iniciado, execute em outro PowerShell:

```powershell
$body = @{
    name = 'Ada Lovelace'
    email = 'ada@example.com'
    password = 'a-secure-password'
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri http://localhost:8080/api/users `
    -ContentType 'application/json' `
    -Body $body
```

Depois consulte:

```powershell
Invoke-RestMethod http://localhost:8080/api/users?page=0`&size=20
```

## 9. Decisões para as próximas fases

- autenticar o usuário e emitir JWT;
- restringir consulta, atualização e remoção conforme identidade e permissões;
- criar fluxo autenticado de troca de senha;
- aplicar a identidade autenticada às permissões de workspace;
- versionar e documentar o contrato com OpenAPI.

## 10. Como explicar em uma entrevista

> Criei uma API em camadas que não expõe a entidade persistente. Os DTOs validam o contrato, o serviço concentra transações e unicidade, e os erros seguem Problem Details. A senha é transformada por PBKDF2 antes da entidade ser criada, nunca aparece nas respostas, e um teste de integração comprova o hash no SQL Server real.
