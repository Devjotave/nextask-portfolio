# Fase 9 — Workspaces e quadro Kanban

## 1. Objetivo

Transformar a área autenticada em uma interface funcional de gerenciamento de trabalho. A pessoa usuária pode criar seu primeiro workspace, adicionar um projeto, cadastrar tarefas e acompanhar o fluxo delas entre as colunas do Kanban usando os dados reais da API.

Esta fase mantém o escopo concentrado no fluxo principal. Gerenciamento de membros, comentários e edição detalhada das tarefas permanecem para o próximo incremento.

## 2. Fluxo da área autenticada

Após o login, o frontend executa o seguinte fluxo:

1. consulta os workspaces da pessoa autenticada;
2. orienta a criação do primeiro workspace quando a lista está vazia;
3. carrega os projetos do workspace selecionado;
4. orienta a criação do primeiro projeto quando necessário;
5. carrega as tarefas do projeto e as distribui por estado;
6. permite criar e mover tarefas sem recarregar a página.

As seleções de workspace e projeto ficam explícitas na interface. Ao trocar uma delas, os dados dependentes são carregados novamente para evitar a mistura de informações entre contextos.

## 3. Contratos consumidos

| Ação | Método e endpoint |
|---|---|
| Listar workspaces | `GET /api/workspaces` |
| Criar workspace | `POST /api/workspaces` |
| Listar projetos | `GET /api/workspaces/{workspaceId}/projects` |
| Criar projeto | `POST /api/workspaces/{workspaceId}/projects` |
| Listar tarefas | `GET /api/projects/{projectId}/tasks` |
| Criar tarefa | `POST /api/projects/{projectId}/tasks` |
| Atualizar tarefa | `PUT /api/tasks/{taskId}` |

Clientes TypeScript separados modelam paginação, workspaces, projetos e tarefas. Todas as chamadas protegidas passam pelo mesmo cliente autenticado, que inclui o token JWT no cabeçalho `Authorization`.

## 4. Quadro Kanban

As tarefas são agrupadas nas colunas:

- **A fazer** (`TODO`);
- **Em andamento** (`IN_PROGRESS`);
- **Concluído** (`DONE`).

Cada cartão apresenta título, prioridade e prazo, quando informado. A movimentação pode ser feita por arrastar e soltar ou pelos botões de seta, permitindo que o mesmo fluxo seja usado sem depender de mouse ou gestos de precisão.

Ao mover uma tarefa, o frontend envia o estado completo exigido pelo contrato de atualização. A interface só confirma a mudança após a resposta da API.

## 5. Criação guiada

Modais reutilizáveis concentram os formulários de workspace, projeto e tarefa. Eles possuem título acessível, fechamento pela tecla `Escape`, fechamento pelo fundo da janela e bloqueio dos botões enquanto uma solicitação está em andamento.

Os campos respeitam as validações básicas do backend. A tarefa recebe título, descrição opcional, prioridade e prazo opcional; seu estado inicial é sempre **A fazer**.

## 6. Carregamento, erros e segurança

A interface distingue carregamento inicial, listas vazias e falhas. Erros no formato Problem Details continuam sendo exibidos em linguagem direta.

- uma resposta `401` encerra a sessão local e redireciona para o login;
- uma resposta `409` durante a movimentação recarrega as tarefas, evitando manter uma versão desatualizada do quadro;
- nenhuma credencial é registrada no código ou enviada em parâmetros de URL;
- workspace, projeto e tarefa sempre são consultados dentro do contexto autenticado.

## 7. Responsividade e acessibilidade

No desktop, a área interna usa barra lateral, cabeçalho contextual e três colunas. Em telas menores, os controles são reorganizados e as colunas continuam navegáveis horizontalmente sem comprimir os cartões.

Formulários usam labels reais, mensagens de erro são anunciadas com `role="alert"`, o foco por teclado permanece visível e os controles de movimentação possuem nomes acessíveis.

## 8. Validar

No diretório `frontend`:

```powershell
pnpm lint
pnpm test
pnpm build
```

Resultado final da fase:

- 12 testes do frontend aprovados em 2 arquivos;
- lint sem avisos;
- verificação estrita de tipos aprovada;
- build de produção Vite aprovado;
- fluxos automatizados de criação de workspace, projeto e tarefa aprovados;
- movimentação entre colunas e expiração da sessão cobertas por testes.

## 9. Próxima fase

Adicionar ao frontend a visualização detalhada da tarefa, comentários e gerenciamento dos membros do workspace, aproveitando os contratos que já existem no backend.

## 10. Como explicar em uma entrevista

> Evoluí a área autenticada para consumir os recursos centrais do produto. Modelei clientes TypeScript para os contratos paginados, construí um onboarding progressivo para workspaces e projetos e implementei um Kanban que atualiza tarefas reais pela API. Além do arrastar e soltar, ofereci controles por teclado, tratei expiração do JWT e conflito de versão e cobri o fluxo completo com testes de comportamento.
