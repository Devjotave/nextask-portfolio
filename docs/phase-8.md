# Fase 8 — React, autenticação e navegação

## 1. Objetivo

Criar o primeiro frontend funcional do NexTask. Esta fase transforma os endpoints de autenticação das fases anteriores em uma experiência de cadastro e login, mantém a sessão JWT durante a aba atual e protege a área interna da aplicação.

O quadro Kanban não foi antecipado: a Fase 8 entrega a fundação necessária para que workspaces, projetos e tarefas sejam adicionados no próximo incremento sem misturar responsabilidades.

## 2. Stack escolhida

- React 19 com TypeScript 6;
- Vite 8 para desenvolvimento e build;
- React Router 7 em modo declarativo;
- Vitest, jsdom e Testing Library para testes;
- Oxlint para verificação estática;
- pnpm 11 com lockfile versionado.

O projeto exige Node.js 22.13 ou superior. Essa versão atende simultaneamente aos requisitos do Vite e do pnpm usado pelo repositório.

Referências oficiais:

- [Vite — Getting Started](https://vite.dev/guide/)
- [Template React + TypeScript mantido pelo Vite](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts)
- [React Router — instalação declarativa](https://reactrouter.com/start/declarative/installation)
- [Vitest — Getting Started](https://vitest.dev/guide/)
- [pnpm — instalação e compatibilidade](https://pnpm.io/installation)

## 3. Estrutura do frontend

```text
frontend/
├── src/
│   ├── api/          cliente HTTP e contratos de autenticação
│   ├── auth/         contexto, armazenamento da sessão e guards
│   ├── components/   marca e estrutura visual compartilhada
│   ├── pages/        login, cadastro, área autenticada e 404
│   ├── test/         preparação do ambiente de testes
│   ├── App.tsx       rotas da aplicação
│   └── styles.css    design responsivo
├── vite.config.ts    proxy local para o backend
└── vitest.config.ts  ambiente jsdom
```

## 4. Rotas

| Rota | Acesso | Comportamento |
|---|---|---|
| `/` | Todos | Redireciona conforme o estado da sessão |
| `/login` | Visitante | Autentica em `POST /api/auth/login` |
| `/register` | Visitante | Cria a conta em `POST /api/auth/register` |
| `/app` | Autenticado | Exibe perfil e estado inicial do produto |
| `*` | Todos | Página 404 |

`ProtectedRoute` envia visitantes para o login e preserva a origem da navegação. `PublicOnlyRoute` impede que uma pessoa já autenticada volte às telas de entrada. A troca de rota também restaura a rolagem ao topo, inclusive em telas pequenas.

## 5. Cliente HTTP e erros

O cliente usa `fetch` e envia JSON para os endpoints já publicados. Em desenvolvimento, as URLs são relativas e o Vite encaminha `/api` para `http://localhost:8080`; dessa forma, não é necessário liberar CORS no backend apenas para o ambiente local.

Quando a API retorna `ProblemDetail`, o frontend apresenta `detail` ao usuário. Falhas de rede recebem uma mensagem específica orientando a verificar se o backend está ligado. A variável opcional `VITE_API_URL` permite apontar o build para outro endereço, mas nunca deve receber senhas, tokens ou outras informações privadas.

## 6. Sessão e segurança

O `AuthProvider` concentra login, cadastro e logout. Após uma resposta válida, ele armazena na sessão:

- token de acesso;
- tipo do token;
- instante calculado de expiração;
- perfil público retornado pela API.

O armazenamento escolhido é `sessionStorage`. Ele permite atualizar a página sem perder a autenticação e remove os dados quando a aba é fechada. Sessões expiradas ou corrompidas são descartadas antes da renderização.

Essa escolha é adequada ao contrato JWT atual, mas não torna o token imune a XSS. React escapa texto renderizado por padrão, nenhuma credencial é inserida como HTML e o projeto não usa `dangerouslySetInnerHTML`. Uma evolução futura pode adotar cookies `HttpOnly` e refresh tokens quando o backend oferecer esse contrato.

## 7. Interface e acessibilidade

As telas de autenticação usam labels reais, tipos de campo e atributos de preenchimento automático adequados. Erros são anunciados com `role="alert"`, botões exibem estado de carregamento e o foco por teclado permanece visível.

O layout foi verificado em desktop e em viewport móvel de 390 × 844 pixels. A área autenticada não inventa workspaces ou tarefas: ela confirma a integração e mostra claramente que o Kanban pertence ao próximo incremento.

## 8. Executar

Mantenha o backend ativo na porta 8080 e abra outro PowerShell:

```powershell
Set-Location frontend
pnpm install
pnpm dev
```

Acesse `http://localhost:5173`.

## 9. Validar

```powershell
pnpm lint
pnpm test
pnpm build
```

Resultado final da fase:

- 9 testes do frontend aprovados em 2 arquivos;
- lint sem avisos;
- verificação estrita de tipos aprovada;
- build de produção Vite aprovado;
- login e cadastro verificados visualmente em desktop e celular;
- console do navegador sem erros ou avisos.

## 10. Próxima fase

Consumir workspaces, projetos e tarefas na área autenticada e construir o primeiro quadro Kanban com estados reais da API.

## 11. Como explicar em uma entrevista

> Construí o frontend React sobre os contratos OpenAPI já existentes. Centralizei a sessão JWT em um contexto, criei guards para separar rotas públicas e protegidas e usei um cliente HTTP que entende os erros Problem Details do Spring. Mantive o token somente na sessão da aba, configurei proxy local em vez de abrir CORS sem necessidade e testei os fluxos de autenticação pelo comportamento visível ao usuário.
