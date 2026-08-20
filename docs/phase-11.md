# Fase 11 — Preparação para deploy e integração contínua

## Objetivo

Transformar o NexTask em uma aplicação reproduzível fora da máquina de desenvolvimento. A fase cria imagens imutáveis para o frontend e o backend, reúne todos os serviços em Docker Compose e impede que alterações quebradas sejam publicadas sem passar pelos testes automáticos.

## Arquitetura de produção

```text
Navegador :8080
      |
      v
NGINX sem privilégios
  |            |
  | /api       | arquivos React
  v            v
Spring Boot   SPA
      |
      v
SQL Server 2022
```

O navegador acessa somente o NGINX. Requisições `/api` são encaminhadas internamente ao backend; as demais rotas entregam o frontend React e possuem fallback para `index.html`. Assim, frontend e API compartilham a mesma origem e não exigem uma configuração pública de CORS.

## Imagens de produção

Os dois `Dockerfile` usam construção em múltiplos estágios:

- o backend é compilado com Maven e Java 21; a imagem final contém apenas o Java Runtime e executa com um usuário sem privilégios;
- o frontend é validado e compilado com Node.js e pnpm; a imagem final contém somente o NGINX e os arquivos estáticos;
- ambos possuem verificação de saúde para que a orquestração saiba quando estão prontos;
- arquivos locais, dependências instaladas e segredos são excluídos dos contextos de construção.

O perfil Spring `production` habilita encerramento gracioso, limita o pool de conexões, respeita cabeçalhos do proxy e desabilita OpenAPI por padrão.

## Docker Compose

O arquivo `compose.yml` descreve quatro serviços:

1. `database` inicia o SQL Server e mantém os dados em um volume;
2. `database-init` aguarda o banco ficar saudável e aplica o script idempotente do schema;
3. `backend` inicia somente depois da preparação bem-sucedida do banco;
4. `frontend` inicia quando o backend está saudável e publica a porta 8080.

As senhas e a chave JWT ficam apenas em `.env.compose`, ignorado pelo Git. Para gerar esse arquivo sem exibir segredos:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-compose-env.ps1
```

Depois, com o Docker Desktop aberto:

```powershell
docker compose --env-file .env.compose up --build
```

O NexTask fica disponível em `http://localhost:8080`. Para encerrar sem apagar os dados:

```powershell
docker compose --env-file .env.compose down
```

## Integração contínua

O GitHub Actions executa três trabalhos independentes:

| Trabalho | Verificações |
| --- | --- |
| Backend | Java 21, cache do Maven e testes rápidos |
| Frontend | Node.js 24, pnpm 11, lint, 14 testes e build TypeScript/Vite |
| Imagens | validação do Compose e construção das imagens do backend e frontend |

O terceiro trabalho só começa quando backend e frontend passam. O workflow possui permissões somente de leitura, cancela execuções antigas da mesma alteração e não usa segredos reais. O Dependabot verifica semanalmente novas versões das ações e imagens base.

## Decisões de segurança

- nenhum segredo é incluído nas imagens ou no repositório;
- backend e NGINX executam sem usuário administrador;
- o filesystem do backend fica somente para leitura, com `/tmp` temporário;
- o SQL Server não publica sua porta para a máquina hospedeira;
- somente o NGINX expõe uma porta pública;
- Swagger/OpenAPI fica desabilitado por padrão no perfil de produção;
- o schema continua validado pelo Hibernate e criado pelo script SQL explícito.

## Critério de conclusão

A fase está concluída quando testes e builds locais passam, a configuração do Compose é válida, as imagens são construídas e o workflow é aceito pelo GitHub após a publicação do commit. O próximo passo é escolher uma plataforma, cadastrar os segredos nela e realizar o primeiro deploy público.

## Como apresentar esta fase

> Preparei o NexTask para execução reproduzível com imagens multi-stage, processos sem privilégios, health checks e inicialização ordenada do SQL Server. O NGINX entrega a SPA e funciona como proxy da API na mesma origem. Também criei uma pipeline no GitHub Actions que testa as duas aplicações e constrói as imagens antes de aceitar a entrega.
