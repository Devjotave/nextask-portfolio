# Fase 2 — Modelagem de usuários

## 1. Objetivo

Introduzir o primeiro conceito de domínio persistente do NexTask: a tabela `users` no SQL Server e sua representação como entidade JPA `User`.

Esta fase cuida somente do modelo de dados, das invariantes básicas e da persistência. Repository, casos de uso, endpoints, DTOs, autenticação e hash de senha serão implementados nas fases apropriadas. A entidade já recebe exclusivamente `passwordHash` para impedir que uma senha em texto puro faça parte do estado persistido.

## 2. Modelo da tabela

| Coluna | Tipo SQL Server | Regra |
|---|---|---|
| `id` | `bigint IDENTITY` | Chave primária gerada pelo banco |
| `name` | `nvarchar(120)` | Obrigatório e não pode ser vazio |
| `email` | `nvarchar(254)` | Obrigatório, normalizado e único |
| `password_hash` | `nvarchar(255)` | Obrigatório; armazena apenas o hash codificado |
| `created_at` | `datetimeoffset(6)` | Instante de criação em UTC |
| `updated_at` | `datetimeoffset(6)` | Instante da última alteração em UTC |

`bigint` oferece amplo espaço de crescimento e funciona diretamente com `Long` no Java. `nvarchar` preserva nomes internacionais. O limite de 254 caracteres do e-mail segue o tamanho máximo prático de um endereço completo, enquanto 255 comporta formatos codificados de hash como BCrypt e Argon2.

A constraint única protege o e-mail no ponto definitivo de concorrência: o banco. A aplicação também remove espaços externos e converte o endereço para minúsculas antes de persistir, mantendo uma representação canônica.

## 3. Integridade no banco

O script `database/init-database.sql` continua idempotente. Ele cria `dbo.users` somente quando a tabela ainda não existe e declara:

- chave primária clusterizada em `id`;
- unicidade de `email`;
- colunas obrigatórias;
- checks contra textos vazios;
- valores padrão UTC para as datas.

Os defaults de data protegem inserções feitas diretamente no banco. Nas operações JPA, os callbacks da entidade definem os mesmos valores, permitindo que o objeto Java reflita seu estado persistido imediatamente.

## 4. Entidade JPA

A classe `User` usa acesso por campos e não expõe setters genéricos. O construtor exige os três dados necessários e as mudanças passam por operações explícitas:

- `rename`;
- `changeEmail`;
- `changePasswordHash`.

Essas operações rejeitam nulos, textos em branco e valores maiores que as respectivas colunas. Bean Validation complementa as invariantes com `@Email`, `@NotBlank` e `@Size`. O construtor protegido sem argumentos existe apenas para o JPA.

Os callbacks `@PrePersist` e `@PreUpdate` mantêm `createdAt` e `updatedAt` em UTC. Não foi usado Lombok: neste estágio, o código explícito deixa visíveis o ciclo de vida e as regras da entidade.

## 5. Preparar o banco

Na raiz do repositório, execute novamente o script idempotente:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\database\setup-database.ps1
```

Além de manter banco, login e permissões, ele agora cria `dbo.users`. O resultado esperado termina com:

```text
NexTask database, schema, and application login are ready.
```

O `Bypass` é aplicado somente ao processo que executa o script; a política do PowerShell não é alterada permanentemente. Só avance para os testes de integração depois de ver a mensagem acima.

## 6. Testes

Os testes rápidos não dependem do SQL Server:

```powershell
Set-Location backend
.\mvnw.cmd test
```

`UserTest` verifica normalização, campos obrigatórios, limites, formato de e-mail e operações de alteração.

Depois de preparar o banco, execute os testes reais:

```powershell
.\mvnw.cmd verify -Pintegration-test
```

Além da conexão criada na Fase 1, `UserPersistenceIT` insere a entidade, força o SQL, limpa o contexto JPA e lê a linha novamente. Isso comprova em conjunto o mapeamento, o `IDENTITY`, os tipos e os timestamps. A transação do teste é revertida automaticamente.

## 7. Decisões deixadas para as próximas fases

- O hash real de senha será produzido pelo serviço de cadastro quando Spring Security for introduzido.
- Validações de entrada e mensagens de API pertencerão aos DTOs, sem expor a entidade diretamente.
- Consultas serão adicionadas por um `UserRepository` quando surgir o primeiro caso de uso.
- Migrações versionadas serão adotadas antes de existirem ambientes compartilhados; por enquanto, o script idempotente mantém a preparação local coerente com a Fase 1.

## 8. Como explicar em uma entrevista

> Modelei usuários com regras coerentes no banco e no domínio. O SQL Server garante identidade, obrigatoriedade e unicidade, enquanto a entidade normaliza o e-mail, limita os campos e nunca armazena senha pura. Os timestamps usam UTC, e um teste de integração persiste e relê a entidade para validar o mapeamento real, não apenas as anotações.
