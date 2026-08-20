# NexTask

Full Stack project management platform with Kanban boards, workspaces, teams and task collaboration.

## Status

Em desenvolvimento — Fase 7 concluída: contrato OpenAPI e Swagger UI autenticável.

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
- Spring Security / PBKDF2
- OAuth2 Resource Server / JWT
- Springdoc OpenAPI / Swagger UI
- Spring Boot Actuator
- Microsoft SQL Server 2022 Express
- JUnit 5

O backend possui autenticação stateless com tokens JWT assinados por HMAC e documentação OpenAPI navegável. React entra nas próximas fases, junto com código e testes que justifiquem suas dependências.

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

2. Troque a senha de exemplo em `.env` e gere uma chave JWT privada. O arquivo é ignorado pelo Git.

   ```powershell
   $jwtBytes = New-Object byte[] 32
   [Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
   $jwtSecret = [Convert]::ToBase64String($jwtBytes)
   (Get-Content .env) -replace '^JWT_SECRET=.*$', "JWT_SECRET=$jwtSecret" | Set-Content .env
   ```

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

7. Abra a documentação interativa:

   ```text
   http://localhost:8080/swagger-ui.html
   ```

   Use `POST /api/auth/register` ou `POST /api/auth/login`, copie apenas o valor de `accessToken` e informe-o no botão **Authorize**. A interface adiciona o prefixo `Bearer` automaticamente.

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

Os testes confirmam 37 cenários rápidos e 16 cenários de integração, cobrindo autenticação, emissão e validação de tokens, identidade autenticada, documentação OpenAPI, regras `OWNER`/`MEMBER` e os fluxos HTTP completos no SQL Server.

## Segurança local

- `.env` nunca deve ser enviado ao GitHub.
- `JWT_SECRET` deve conter pelo menos 32 bytes aleatórios codificados em Base64 e nunca deve ser publicado.
- Tokens inválidos ou ausentes recebem `401`; operações sem permissão no workspace recebem `403`.
- A documentação fica ativa por padrão no ambiente local e pode ser desabilitada com `OPENAPI_ENABLED=false`.
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
- [`docs/phase-6.md`](docs/phase-6.md): Spring Security, JWT, identidade autenticada e autorização.
- [`docs/phase-7.md`](docs/phase-7.md): contrato OpenAPI, Swagger UI e testes da documentação.

## Roadmap resumido

- [x] Estrutura inicial do repositório
- [x] Backend Spring Boot
- [x] Script reproduzível de configuração do SQL Server
- [x] Validar banco e aplicação no ambiente local
- [x] Modelagem persistente de usuários
- [x] API de usuários
- [x] Workspaces e permissões
- [x] Projetos, tarefas e comentários
- [x] Spring Security e JWT
- [x] Swagger/OpenAPI
- [ ] React e quadro Kanban
- [ ] Deploy
