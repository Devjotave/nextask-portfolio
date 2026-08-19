# Fase 5 — Projetos, tarefas e comentários

## 1. Objetivo

Construir o núcleo de colaboração que sustentará o quadro Kanban. Workspaces passam a conter projetos; projetos contêm tarefas; e tarefas recebem comentários. Criadores, responsáveis e autores são vinculados a usuários existentes e validados como membros do workspace correspondente no momento da operação.

A fase ainda não identifica automaticamente quem chamou a API. Os campos `createdById` e `authorId` são temporários até Spring Security e JWT fornecerem a identidade autenticada.

## 2. Modelo relacional

### `dbo.projects`

Cada projeto pertence a um workspace e registra nome, descrição opcional, criador e timestamps. A remoção do workspace elimina seus projetos em cascata.

### `dbo.tasks`

Cada tarefa pertence a um projeto e possui:

- título e descrição opcional;
- status `TODO`, `IN_PROGRESS` ou `DONE`;
- prioridade `LOW`, `MEDIUM` ou `HIGH`;
- posição inteira não negativa para a ordenação do futuro Kanban;
- responsável opcional e criador obrigatório;
- prazo opcional;
- versão, criação e última atualização.

As constraints do SQL Server restringem status, prioridade e posição. Os índices por projeto/status/posição e por responsável atendem às consultas principais do quadro.

### `dbo.comments`

Cada comentário pertence a uma tarefa, registra autor, conteúdo e timestamps. Excluir uma tarefa remove seus comentários; excluir um projeto remove suas tarefas e, por consequência, todos os comentários abaixo delas.

As FKs para usuários usam `NO ACTION`. Assim, conteúdo histórico não perde silenciosamente o autor ou criador quando alguém tenta excluir uma conta.

## 3. Fronteira do workspace

Antes de criar um projeto, o serviço confirma que `createdById` pertence ao workspace. A mesma regra é aplicada ao criador e ao responsável de uma tarefa, e ao autor de um comentário.

Um usuário existente, mas externo ao workspace, recebe `409 Conflict` com o tipo `urn:nextask:problem:user-must-belong-to-workspace`. Essa validação evita referências cruzadas entre equipes antes mesmo da fase de autenticação.

Criadores e autores formam histórico imutável. O responsável da tarefa pode ser trocado ou removido em uma atualização completa.

## 4. Concorrência de tarefas

Tarefas são os registros com maior chance de edição simultânea no quadro. A propriedade `version`, mapeada com `@Version`, faz o Hibernate incluir a versão conhecida na atualização e incrementá-la quando a gravação vence.

Se outra transação tiver alterado a mesma tarefa, a API devolve `409 Conflict` em vez de sobrescrever silenciosamente dados mais novos. O funcionamento de atributos de versão é descrito na [documentação oficial do Hibernate](https://docs.jboss.org/hibernate/orm/6.6/introduction/pdf/Hibernate_Introduction.pdf).

## 5. Endpoints de projetos

| Método | Caminho | Resultado |
|---|---|---|
| `POST` | `/api/workspaces/{workspaceId}/projects` | cria projeto e retorna `201` |
| `GET` | `/api/workspaces/{workspaceId}/projects` | lista projetos paginados |
| `GET` | `/api/projects/{projectId}` | consulta projeto |
| `PUT` | `/api/projects/{projectId}` | atualiza nome e descrição |
| `DELETE` | `/api/projects/{projectId}` | remove projeto e seus descendentes |

## 6. Endpoints de tarefas

| Método | Caminho | Resultado |
|---|---|---|
| `POST` | `/api/projects/{projectId}/tasks` | cria tarefa inicialmente em `TODO` |
| `GET` | `/api/projects/{projectId}/tasks` | lista por posição e `id` |
| `GET` | `/api/projects/{projectId}/tasks?status=TODO` | filtra por status |
| `GET` | `/api/tasks/{taskId}` | consulta tarefa |
| `PUT` | `/api/tasks/{taskId}` | atualiza conteúdo, status, prioridade, posição, responsável e prazo |
| `DELETE` | `/api/tasks/{taskId}` | remove tarefa e comentários |

## 7. Endpoints de comentários

| Método | Caminho | Resultado |
|---|---|---|
| `POST` | `/api/tasks/{taskId}/comments` | cria comentário e retorna `201` |
| `GET` | `/api/tasks/{taskId}/comments` | lista comentários paginados |
| `PUT` | `/api/comments/{commentId}` | edita o conteúdo |
| `DELETE` | `/api/comments/{commentId}` | remove comentário |

Todas as listagens aceitam `page` a partir de zero e `size` entre 1 e 100.

## 8. Exemplo de fluxo

```http
POST /api/projects/20/tasks
Content-Type: application/json

{
  "title": "Implementar API",
  "description": "Contrato de tarefas",
  "priority": "HIGH",
  "position": 0,
  "assigneeId": 2,
  "createdById": 1,
  "dueAt": "2026-09-01T18:00:00Z"
}
```

A resposta contém `status: "TODO"` e `version: 0`. Uma atualização pode mover a tarefa:

```http
PUT /api/tasks/30
Content-Type: application/json

{
  "title": "Implementar API",
  "description": "Contrato atualizado",
  "status": "IN_PROGRESS",
  "priority": "MEDIUM",
  "position": 1,
  "assigneeId": null,
  "dueAt": null
}
```

## 9. Testes

Depois de atualizar o schema, execute:

```powershell
Set-Location backend
.\mvnw.cmd clean test
.\mvnw.cmd verify -Pintegration-test
```

Os testes rápidos cobrem criação e validação de membros, status inicial, contratos HTTP e conflito de versão. `CollaborationApiIT` percorre o fluxo real no SQL Server, filtra tarefas, atualiza status e comentário, rejeita usuários externos e comprova a cascata projeto → tarefa → comentário.

Validação concluída no ambiente local com 31 testes rápidos e 12 testes de integração, todos aprovados sem falhas ou erros.

## 10. Próxima fase

Spring Security e JWT substituirão os identificadores declarados pelo cliente pela identidade autenticada. A autorização deverá então limitar criação, edição e remoção conforme o papel no workspace, sem alterar as relações persistentes construídas nesta fase.

## 11. Como explicar em uma entrevista

> Modelei o agregado de colaboração em três níveis e mantive a fronteira do workspace nos serviços. O banco protege enums, referências e cascatas; a API pagina as coleções e não expõe entidades JPA. Nas tarefas usei versionamento otimista para detectar atualizações concorrentes e evitar perda silenciosa de dados. O fluxo completo é comprovado contra SQL Server real.
