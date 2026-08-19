# Fase 4 — Workspaces e permissões

## 1. Objetivo

Modelar a primeira fronteira colaborativa do NexTask. Um workspace reúne usuários e atribui a cada associação um papel. A API permite criar, consultar, renomear e remover workspaces, além de listar, adicionar, promover, rebaixar e remover seus membros.

Esta fase modela as permissões, mas ainda não autentica quem faz a requisição. Até a implementação de Spring Security e JWT, os endpoints ficam abertos somente para desenvolvimento local e não representam uma autorização pronta para produção.

## 2. Modelo relacional

O script idempotente `database/init-database.sql` acrescenta duas tabelas:

### `dbo.workspaces`

| Coluna | Tipo | Regra |
|---|---|---|
| `id` | `bigint IDENTITY` | chave primária |
| `name` | `nvarchar(120)` | obrigatório e não vazio |
| `created_at` | `datetimeoffset(6)` | instante UTC de criação |
| `updated_at` | `datetimeoffset(6)` | instante UTC da última alteração |

### `dbo.workspace_members`

| Coluna | Tipo | Regra |
|---|---|---|
| `workspace_id` | `bigint` | FK para `workspaces` |
| `user_id` | `bigint` | FK para `users` |
| `role` | `varchar(20)` | somente `OWNER` ou `MEMBER` |
| `joined_at` | `datetimeoffset(6)` | instante UTC da entrada |

A chave primária composta `(workspace_id, user_id)` impede que o mesmo usuário seja adicionado duas vezes ao mesmo workspace. O índice iniciado por `user_id` acelera a listagem dos workspaces de um usuário.

Ao remover um workspace, suas associações são excluídas em cascata. A exclusão de um usuário associado é bloqueada pela FK e convertida pela API em `409 Conflict`, evitando perda implícita de participação ou propriedade.

## 3. Entidades e papéis

`WorkspaceMemberId` representa a chave composta com `@EmbeddedId`. `WorkspaceMember` associa `Workspace` e `User` com `@MapsId`, mantendo as duas FKs como partes da identidade da associação.

Existem dois papéis:

- `OWNER`: proprietário com responsabilidade administrativa;
- `MEMBER`: participante comum.

Um workspace é criado na mesma transação que sua primeira associação, sempre atribuindo `OWNER` ao usuário informado por `ownerId`. Mais de um proprietário é permitido.

## 4. Invariante de proprietário

Um workspace existente nunca pode ficar sem proprietário. Rebaixar ou remover seu último `OWNER` retorna `409 Conflict`.

Uma simples contagem não protegeria duas requisições simultâneas: ambas poderiam observar dois proprietários e remover um cada. Por isso, essas operações obtêm um bloqueio pessimista de escrita na linha do workspace antes de contar e alterar as associações. As requisições concorrentes do mesmo workspace ficam serializadas dentro das respectivas transações. O suporte de `@Lock` em métodos de repositório é documentado pela [referência oficial do Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html).

## 5. Endpoints

| Método | Caminho | Resultado |
|---|---|---|
| `POST` | `/api/workspaces` | cria workspace e proprietário, retornando `201` |
| `GET` | `/api/workspaces?userId={id}&page=0&size=20` | lista workspaces e o papel do usuário |
| `GET` | `/api/workspaces/{workspaceId}` | consulta um workspace |
| `PUT` | `/api/workspaces/{workspaceId}` | renomeia um workspace |
| `DELETE` | `/api/workspaces/{workspaceId}` | remove workspace e associações |
| `GET` | `/api/workspaces/{workspaceId}/members` | lista membros e papéis |
| `POST` | `/api/workspaces/{workspaceId}/members` | adiciona membro, retornando `201` |
| `PUT` | `/api/workspaces/{workspaceId}/members/{userId}` | altera o papel |
| `DELETE` | `/api/workspaces/{workspaceId}/members/{userId}` | remove a associação |

As duas listagens usam paginação a partir de zero, com tamanho entre 1 e 100.

## 6. Exemplos

Criação:

```http
POST /api/workspaces
Content-Type: application/json

{
  "name": "Product Team",
  "ownerId": 1
}
```

Adição de membro:

```http
POST /api/workspaces/10/members
Content-Type: application/json

{
  "userId": 2,
  "role": "MEMBER"
}
```

Promoção:

```http
PUT /api/workspaces/10/members/2
Content-Type: application/json

{
  "role": "OWNER"
}
```

## 7. Erros HTTP

As respostas continuam usando `ProblemDetail`:

- `400 Bad Request`: campos inválidos ou papel desconhecido;
- `404 Not Found`: usuário, workspace ou associação inexistente;
- `409 Conflict`: associação duplicada, último proprietário ou exclusão de usuário associado.

O tratador e a resposta paginada foram movidos para `com.nextask.api`, pois agora são compartilhados pelos módulos `user` e `workspace`.

## 8. Validação

Primeiro atualize o schema. O comando pode ser executado novamente com segurança:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\database\setup-database.ps1
```

Depois execute os testes rápidos:

```powershell
Set-Location backend
.\mvnw.cmd test
```

E a integração real com SQL Server:

```powershell
.\mvnw.cmd verify -Pintegration-test
```

`WorkspaceApiIT` comprova a criação automática do proprietário, paginação, promoção, proteção do último `OWNER`, remoção de membros, renomeação, cascata e bloqueio da exclusão de usuário associado.

Resultado da validação da fase: 22 testes rápidos e 9 testes de integração, sem falhas, erros ou cenários ignorados.

## 9. Próximos limites de segurança

O corpo ainda recebe `ownerId` e a listagem recebe `userId`, pois não existe identidade autenticada. Na fase de segurança, o backend deverá extrair o usuário do JWT, verificar sua associação e exigir `OWNER` nas operações administrativas. Os papéis e invariantes criados aqui serão a base dessas decisões.

## 10. Como explicar em uma entrevista

> Modelei membros de workspace como uma entidade de associação com chave composta e papel próprio. A criação do workspace e do primeiro proprietário é atômica. Para impedir que duas requisições concorrentes removam todos os proprietários, serializo mudanças críticas com bloqueio pessimista e valido a quantidade de owners dentro da transação. FKs, constraints e testes no SQL Server reforçam as regras da aplicação.
