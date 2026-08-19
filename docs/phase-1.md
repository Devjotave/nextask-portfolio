# Fase 1 — Preparação do NexTask

## 1. Objetivo

Construir uma base pequena e verificável: repositório Git, backend Spring Boot, SQL Server reproduzível, configuração sem credenciais versionadas e um teste de integração que prove a conexão.

Nenhuma regra de usuários, autenticação, Workspace, projeto ou tarefa pertence a esta fase.

## 2. Ferramentas

Confirme no terminal:

```powershell
java -version
mvn -version
git --version
sqlcmd -?
```

O projeto usa Java 21 LTS. O Spring Boot 3.5 exige no mínimo Java 17, mas Java 21 oferece uma base LTS moderna para portfólio.

## 3. Configuração do Git

Configure sua identidade uma única vez na máquina, usando seus próprios dados:

```powershell
git config --global user.name "Seu Nome"
git config --global user.email "seu-email@example.com"
git config --global init.defaultBranch main
git config --global core.autocrlf true
```

- `user.name` e `user.email` identificam a autoria dos commits.
- `init.defaultBranch main` padroniza o nome da branch principal.
- `core.autocrlf true` evita diferenças desnecessárias de quebra de linha no Windows; o `.gitattributes` do repositório define exceções importantes.

Confira antes de continuar:

```powershell
git config --global --list
```

## 4. Dependências escolhidas

| Dependência | Função nesta fase |
|---|---|
| Spring Web | Base da futura API REST e servidor HTTP incorporado |
| Spring Data JPA | Integração em camadas com persistência relacional |
| Hibernate | Implementação JPA gerenciada pelo Spring Boot |
| Validation | Base para validações de DTOs nas próximas fases |
| Actuator | Endpoint de saúde para validar aplicação e banco |
| Microsoft JDBC Driver | Comunicação Java ↔ SQL Server |
| Spring Boot Test | JUnit 5, AssertJ e suporte aos testes Spring |

Spring Security, JWT e OpenAPI ainda não foram adicionados. Colocar dependências sem implementar seus comportamentos esconderia complexidade e dificultaria o aprendizado incremental.

## 5. Arquivos principais

```text
nextask/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nextask/NexTaskApplication.java
│       ├── main/resources/application.yml
│       └── test/java/com/nextask/
├── database/init-database.sql
├── docs/phase-1.md
├── .env.example
├── .gitignore
└── README.md
```

Os packages `controller`, `service`, `repository`, `entity` e `dto` serão adicionados quando existir a primeira funcionalidade. Diretórios vazios não são rastreados pelo Git e não demonstram arquitetura por si só.

## 6. Como o SQL Server é preparado

Esta fase usa a instância local `SQLEXPRESS`. Docker fica reservado para a Fase 13, conforme o roadmap do projeto. O SQL Server Browser precisa estar ativo para que o driver JDBC descubra a porta dinâmica da instância nomeada.

O PowerShell `database/setup-database.ps1` lê `DB_PASSWORD` do arquivo local `.env` e chama `sqlcmd` com autenticação integrada do Windows. O script SQL executa quatro responsabilidades:

1. `CREATE DATABASE` cria o banco lógico `nextask` quando ele ainda não existe.
2. `CREATE LOGIN` cria uma identidade SQL exclusiva para a aplicação.
3. `CREATE USER` mapeia esse login dentro do banco `nextask`.
4. `ALTER ROLE` concede leitura e escrita ao usuário da aplicação.

Esses comandos são idempotentes: executar a inicialização novamente não duplica banco, login ou usuário.

Ainda não existem tabelas. Elas serão introduzidas com as entidades e migrations das fases funcionais, quando Primary Keys, Foreign Keys, índices e constraints puderem ser explicados no contexto correto.

Antes da execução, o SQL Server deve estar no modo **SQL Server e Windows Authentication**. Essa configuração é conhecida como modo misto. Após alterá-la no SSMS, reinicie `SQL Server (SQLEXPRESS)`.

## 7. Variáveis de ambiente

Copie `.env.example` para `.env` e substitua as senhas:

```powershell
Copy-Item .env.example .env
```

O Spring Boot importa `.env` de forma opcional no ambiente local. O `.gitignore` protege esse arquivo contra commits acidentais.

As propriedades têm funções diferentes:

- `DB_USERNAME` e `DB_PASSWORD`: credenciais de menor privilégio usadas pelo backend.
- `DB_URL`: endereço JDBC, instância, banco, criptografia e certificado do ambiente local.

## 8. Abrir na IDE

Abra a pasta raiz `nextask`, não apenas `src`. Importe `backend/pom.xml` como projeto Maven e selecione o JDK 21.

Classe executável:

```text
backend/src/main/java/com/nextask/NexTaskApplication.java
```

Se a IDE não localizar dependências, atualize/reimporte o projeto Maven antes de alterar o código.

## 9. Validação

Execute na raiz, em um terminal com a mesma conta Windows que administra o SQL Server:

```powershell
.\database\setup-database.ps1
```

O resultado deve terminar com `NexTask database and application login are ready.` Se o script informar que o modo misto está desabilitado, altere a opção no SSMS, reinicie `SQLEXPRESS` e execute novamente.

Depois:

```powershell
Set-Location backend
.\mvnw.cmd test
.\mvnw.cmd verify -Pintegration-test
.\mvnw.cmd spring-boot:run
```

Em outro terminal:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Resultados esperados:

- testes unitários: `BUILD SUCCESS`;
- teste de integração: banco `nextask` e login `nextask_app` confirmados;
- endpoint de saúde: `status` igual a `UP`.

## 10. Primeiro commit

Revise o que será versionado:

```powershell
git status
git diff --check
git check-ignore -v .env
```

O último comando deve confirmar que `.env` está ignorado. Então:

```powershell
git add .
git commit -m "chore: initialize NexTask backend"
```

## 11. Criar e publicar no GitHub

No GitHub, crie um repositório público chamado `nextask` com a descrição:

```text
Full Stack project management platform with Kanban boards, workspaces, teams and task collaboration.
```

Como a base local já contém README e `.gitignore`, não marque as opções para gerar esses arquivos no GitHub. Isso evita históricos independentes e conflitos no primeiro envio.

Na página do repositório vazio, copie a URL HTTPS e execute:

```powershell
git remote add origin https://github.com/SEU-USUARIO/nextask.git
git push -u origin main
```

Confirme no GitHub que `.env` não aparece. O arquivo `.env.example` deve aparecer, pois documenta somente nomes e exemplos de variáveis, sem segredos reais.

## 12. Como explicar em uma entrevista

> Estruturei a primeira fase para ser reproduzível e segura. O SQL Server Express é preparado por um script idempotente, enquanto a aplicação usa um login próprio de menor privilégio. As credenciais ficam em variáveis de ambiente ignoradas pelo Git. Separei um teste rápido de contexto de um teste de integração que confirma o banco e o login reais. Também mantive o Hibernate em modo de validação para impedir alterações silenciosas no schema.
