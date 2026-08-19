# NexTask

Full Stack project management platform with Kanban boards, workspaces, teams and task collaboration.

## Status

Em desenvolvimento — Fase 1: preparação do ambiente e conexão com o banco.

## Objetivo

O NexTask será uma aplicação web para gerenciamento colaborativo de projetos e tarefas. O projeto é construído de forma incremental como portfólio profissional, priorizando aprendizado, segurança, testes e decisões arquiteturais justificadas.

## Arquitetura inicial

```text
React + TypeScript (fase futura)
             ↓
       API REST (fase futura)
             ↓
        Spring Boot 3
             ↓
       Spring Data JPA
             ↓
       Microsoft SQL Server
```

O backend começa como um monólito modular em camadas. Os packages de cada módulo serão criados apenas quando a respectiva funcionalidade for implementada; Git não versiona diretórios vazios.

## Tecnologias da Fase 1

- Java 21 LTS
- Spring Boot 3.5
- Maven
- Spring Web
- Spring Data JPA / Hibernate
- Bean Validation
- Spring Boot Actuator
- Microsoft SQL Server 2022 Express
- JUnit 5

Spring Security, JWT, Swagger e React entram em fases posteriores, junto com código e testes que justifiquem essas dependências.

## Pré-requisitos

- Java 21
- Maven 3.6.3 ou superior, ou Maven Wrapper incluído no projeto
- Git
- SQL Server Express com a instância `SQLEXPRESS`
- SQL Server Management Studio (SSMS) ou o utilitário `sqlcmd`
- IDE com suporte a Java (IntelliJ IDEA, Eclipse/STS ou VS Code)

## Executar localmente

1. Crie o arquivo local de segredos:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Troque a senha de exemplo em `.env`. O arquivo é ignorado pelo Git.

3. Confirme no SQL Server que o modo de autenticação selecionado é **SQL Server e Windows**. Se precisar alterá-lo no SSMS, reinicie o serviço `SQL Server (SQLEXPRESS)`.

4. Prepare o banco usando sua autenticação do Windows:

   ```powershell
   .\database\setup-database.ps1
   ```

   O script cria de forma idempotente o banco `nextask`, o login `nextask_app` e suas permissões mínimas de leitura e escrita.

5. Execute o backend:

   ```powershell
   Set-Location backend
   .\mvnw.cmd spring-boot:run
   ```

6. Em outro terminal, consulte a saúde da aplicação:

   ```powershell
   Invoke-RestMethod http://localhost:8080/actuator/health
   ```

   Resultado esperado:

   ```json
   {"status":"UP"}
   ```

## Testes

Teste rápido, sem depender do banco:

```powershell
Set-Location backend
.\mvnw.cmd test
```

Teste real de integração com SQL Server:

```powershell
.\mvnw.cmd verify -Pintegration-test
```

O teste de integração confirma tanto o banco ativo (`nextask`) quanto o login utilizado pela aplicação (`nextask_app`).

## Segurança local

- `.env` nunca deve ser enviado ao GitHub.
- A aplicação não usa sua conta do Windows nem a conta administrativa `sa`.
- `trustServerCertificate=true` é aceito somente no ambiente local; em produção será usado um certificado confiável.
- O Hibernate está configurado com `ddl-auto=validate`, portanto não altera o schema silenciosamente.

## Documentação

O roteiro explicado da preparação, da configuração do Git e da publicação está em [`docs/phase-1.md`](docs/phase-1.md).

## Roadmap resumido

- [x] Estrutura inicial do repositório
- [x] Backend Spring Boot
- [x] Script reproduzível de configuração do SQL Server
- [x] Validar banco e aplicação no ambiente local
- [ ] Usuários
- [ ] Workspaces e permissões
- [ ] Projetos, tarefas e comentários
- [ ] Spring Security e JWT
- [ ] Swagger/OpenAPI
- [ ] React e quadro Kanban
- [ ] Deploy
