# Fase 10 — Detalhes, comentários e membros

## 1. Objetivo

Completar o primeiro ciclo colaborativo do frontend. Além de visualizar e mover cartões no Kanban, a pessoa usuária agora pode abrir uma tarefa, editar seus detalhes, escolher um responsável entre os membros do workspace e conversar por comentários.

A fase também expõe a composição do workspace. Proprietários podem adicionar pessoas, alterar papéis e remover membros; participantes comuns recebem uma visualização somente de leitura da equipe.

## 2. Painel de detalhes da tarefa

O título de cada cartão abre um painel lateral sem abandonar o quadro. O formulário permite alterar:

- título e descrição;
- estado do Kanban;
- prioridade;
- responsável;
- prazo.

A atualização reaproveita `PUT /api/tasks/{taskId}` e envia o estado completo exigido pelo backend. Quando a coluna é alterada pelo formulário, a tarefa recebe a próxima posição disponível no destino.

O painel fecha pelo botão, pela tecla `Escape` ou pelo fundo. Em telas pequenas ele ocupa toda a largura, mantendo o conteúdo rolável e os controles acessíveis.

## 3. Comentários

O painel carrega os comentários por `GET /api/tasks/{taskId}/comments` e permite:

| Ação | Contrato |
|---|---|
| Comentar | `POST /api/tasks/{taskId}/comments` |
| Editar | `PUT /api/comments/{commentId}` |
| Excluir | `DELETE /api/comments/{commentId}` |

Autores podem gerenciar seus próprios comentários. Proprietários do workspace também recebem os controles de moderação, seguindo a mesma autorização aplicada pelo backend. A exclusão exige confirmação e os erros Problem Details são exibidos dentro do painel.

## 4. Membros e responsáveis

O botão **Pessoas** carrega a lista real de participantes do workspace. Essa lista também alimenta o campo **Responsável** da tarefa, impedindo que a interface sugira pessoas externas ao contexto atual.

O contrato existente adiciona participantes por `userId`, não por e-mail. Por isso, a janela mostra o ID da pessoa autenticada e explica que ele pode ser compartilhado com um proprietário. Somente quem possui papel `OWNER` visualiza os controles de administração:

- adicionar um usuário como `MEMBER` ou `OWNER`;
- promover ou rebaixar participantes;
- remover membros com confirmação.

Regras críticas, como manter ao menos um proprietário, continuam sendo validadas de forma transacional pelo backend.

## 5. Estado, erros e segurança

- respostas `401` encerram a sessão local;
- falhas de validação, permissão e consistência são apresentadas em linguagem direta;
- listas de membros e comentários são recarregadas conforme o workspace ou a tarefa selecionada;
- nenhuma permissão é inferida apenas pela interface: o backend continua sendo a autoridade;
- comentários preservam autoria e horário retornados pela API;
- valores de formulários nunca são inseridos como HTML.

## 6. Acessibilidade e responsividade

O título da tarefa é um botão real, o painel possui semântica de diálogo e todos os campos têm labels. A movimentação do cartão permanece disponível tanto por arrastar e soltar quanto pelos botões direcionais.

No celular, o painel de detalhes vira uma tela integral, os formulários de duas colunas se reorganizam e a gestão de membros mantém nomes, papéis e ações utilizáveis sem rolagem horizontal.

## 7. Validar

No diretório `frontend`:

```powershell
pnpm lint
pnpm test
pnpm build
```

Resultado automatizado da fase:

- 14 testes aprovados em 2 arquivos;
- criação, edição e exclusão de comentários cobertas;
- edição de tarefa, mudança de estado e atribuição de responsável cobertas;
- inclusão de membro pelo proprietário coberta;
- lint sem avisos;
- tipos TypeScript e build de produção aprovados.

## 8. Próxima fase

Preparar a aplicação para deploy, separando configurações por ambiente, definindo a infraestrutura e automatizando a validação antes de cada publicação.

## 9. Como explicar em uma entrevista

> Completei o ciclo colaborativo do Kanban com um painel lateral de edição, atribuição baseada nos membros reais do workspace e comentários com autoria e moderação. A interface diferencia OWNER e MEMBER para melhorar a experiência, mas mantém o backend como autoridade das permissões. Também tratei expiração de sessão, confirmações destrutivas, responsividade e testes dos fluxos completos de colaboração.
