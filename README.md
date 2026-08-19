# NexTask

Full Stack project management platform with Kanban boards, workspaces, teams and task collaboration.

## Status

Em desenvolvimento — Fase 5 concluída: projetos, tarefas e comentários.

## Objetivo

O NexTask será uma aplicação web para gerenciamento colaborativo de projetos e tarefas. O projeto é construído de forma incremental como portfólio profissional, priorizando aprendizado, segurança, testes e decisões arquiteturais justificadas.

## Arquitetura inicial

```text
React + TypeScript (fase futura)
             ↓
       API REST
             ↓
        Spring Boot 3
             ↓
       Spring Data JPA
             ↓
       Microsoft SQL Server
```

O backend começa como um monólito modular em camadas. Os packages de cada módulo serão criados apenas quando a respectiva funcionalidade for implementada; Git não versiona diretórios vazios.

## Tecnologias da base atual

- Java 21 LTS
- Spring Boot 3.5
- Maven
- Spring Web
- Spring Data JPA / Hibernate
- Bean Validation
- Spring Security Crypto / PBKDF2
- Spring Boot Actuator
- Microsoft SQL Server 2022 Express
- JUnit 5

Autenticação com Spring Security, JWT, Swagger e React entram em fases posteriores, junto com código e testes que justifiquem essas dependências. Nesta fase, apenas o módulo criptográfico do Spring Security é usado para impedir o armazenamento de senhas em texto puro.

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
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\database\setup-database.ps1
   ```

   O script cria de forma idempotente o banco `nextask`, o login `nextask_app`, suas permissões mínimas de leitura e escrita e o schema da fase atual.
   Aguarde a mensagem `NexTask database, schema, and application login are ready.` antes de iniciar o backend ou os testes de integração. O `Bypass` vale apenas para esse processo e não altera permanentemente a política do PowerShell.

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

Os testes confirmam 31 cenários rápidos e 12 cenários de integração com o SQL Server, cobrindo o banco ativo (`nextask`), o login da aplicação (`nextask_app`) e os fluxos HTTP de usuários, workspaces, membros, projetos, tarefas e comentários.

## Segurança local

- `.env` nunca deve ser enviado ao GitHub.
- A aplicação não usa sua conta do Windows nem a conta administrativa `sa`.
- `trustServerCertificate=true` é aceito somente no ambiente local; em produção será usado um certificado confiável.
- O Hibernate está configurado com `ddl-auto=validate`, portanto não altera o schema silenciosamente.

## Documentação

Os roteiros explicados estão disponíveis por fase:

- [`docs/phase-1.md`](docs/phase-1.md): preparação, configuração do Git e publicação;
- [`docs/phase-2.md`](docs/phase-2.md): modelagem da tabela `users` e da entidade `User`;
- [`docs/phase-3.md`](docs/phase-3.md): API REST, validação, erros e hash de senha;
- [`docs/phase-4.md`](docs/phase-4.md): workspaces, membros, papéis e consistência transacional;
- [`docs/phase-5.md`](docs/phase-5.md): projetos, tarefas, comentários e concorrência otimista.

## Roadmap resumido

- [x] Estrutura inicial do repositório
- [x] Backend Spring Boot
- [x] Script reproduzível de configuração do SQL Server
- [x] Validar banco e aplicação no ambiente local
- [x] Modelagem persistente de usuários
- [x] API de usuários
- [x] Workspaces e permissões
- [x] Projetos, tarefas e comentários
- [ ] Spring Security e JWT
- [ ] Swagger/OpenAPI
- [ ] React e quadro Kanban
- [ ] Deploy
