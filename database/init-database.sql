:setvar DatabaseName "nextask"
:setvar AppLogin "nextask_app"

USE [master];
GO

IF CONVERT(int, SERVERPROPERTY('IsIntegratedSecurityOnly')) = 1
BEGIN
    THROW 51000, 'Enable SQL Server and Windows Authentication mode, restart SQLEXPRESS, and run this script again.', 1;
END;
GO

IF DB_ID(N'$(DatabaseName)') IS NULL
BEGIN
    CREATE DATABASE [$(DatabaseName)];
END;
GO

DECLARE @loginPassword nvarchar(128) = N'$(DB_PASSWORD)';

IF NOT EXISTS (SELECT 1 FROM sys.sql_logins WHERE name = N'$(AppLogin)')
BEGIN
    DECLARE @createLogin nvarchar(max) =
        N'CREATE LOGIN [$(AppLogin)] WITH PASSWORD = '
        + QUOTENAME(@loginPassword, '''')
        + N', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF, DEFAULT_DATABASE = [$(DatabaseName)];';
    EXEC (@createLogin);
END
ELSE
BEGIN
    DECLARE @alterLogin nvarchar(max) =
        N'ALTER LOGIN [$(AppLogin)] WITH PASSWORD = '
        + QUOTENAME(@loginPassword, '''')
        + N', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF, DEFAULT_DATABASE = [$(DatabaseName)];';
    EXEC (@alterLogin);
END;
GO

USE [$(DatabaseName)];
GO

IF DATABASE_PRINCIPAL_ID(N'$(AppLogin)') IS NULL
BEGIN
    CREATE USER [$(AppLogin)] FOR LOGIN [$(AppLogin)];
END;
GO

IF IS_ROLEMEMBER(N'db_datareader', N'$(AppLogin)') = 0
BEGIN
    ALTER ROLE [db_datareader] ADD MEMBER [$(AppLogin)];
END;
GO

IF IS_ROLEMEMBER(N'db_datawriter', N'$(AppLogin)') = 0
BEGIN
    ALTER ROLE [db_datawriter] ADD MEMBER [$(AppLogin)];
END;
GO

GRANT CONNECT TO [$(AppLogin)];
GO

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[users]
    (
        [id] bigint IDENTITY(1, 1) NOT NULL,
        [name] nvarchar(120) NOT NULL,
        [email] nvarchar(254) NOT NULL,
        [password_hash] nvarchar(255) NOT NULL,
        [created_at] datetimeoffset(6) NOT NULL
            CONSTRAINT [DF_users_created_at] DEFAULT (SYSUTCDATETIME()),
        [updated_at] datetimeoffset(6) NOT NULL
            CONSTRAINT [DF_users_updated_at] DEFAULT (SYSUTCDATETIME()),

        CONSTRAINT [PK_users] PRIMARY KEY CLUSTERED ([id]),
        CONSTRAINT [UQ_users_email] UNIQUE NONCLUSTERED ([email]),
        CONSTRAINT [CK_users_name_not_blank]
            CHECK (LEN(LTRIM(RTRIM([name]))) > 0),
        CONSTRAINT [CK_users_email_not_blank]
            CHECK (LEN(LTRIM(RTRIM([email]))) > 0),
        CONSTRAINT [CK_users_password_hash_not_blank]
            CHECK (LEN(LTRIM(RTRIM([password_hash]))) > 0)
    );
END;
GO

IF OBJECT_ID(N'dbo.workspaces', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[workspaces]
    (
        [id] bigint IDENTITY(1, 1) NOT NULL,
        [name] nvarchar(120) NOT NULL,
        [created_at] datetimeoffset(6) NOT NULL
            CONSTRAINT [DF_workspaces_created_at] DEFAULT (SYSUTCDATETIME()),
        [updated_at] datetimeoffset(6) NOT NULL
            CONSTRAINT [DF_workspaces_updated_at] DEFAULT (SYSUTCDATETIME()),

        CONSTRAINT [PK_workspaces] PRIMARY KEY CLUSTERED ([id]),
        CONSTRAINT [CK_workspaces_name_not_blank]
            CHECK (LEN(LTRIM(RTRIM([name]))) > 0)
    );
END;
GO

IF OBJECT_ID(N'dbo.workspace_members', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[workspace_members]
    (
        [workspace_id] bigint NOT NULL,
        [user_id] bigint NOT NULL,
        [role] varchar(20) NOT NULL,
        [joined_at] datetimeoffset(6) NOT NULL
            CONSTRAINT [DF_workspace_members_joined_at] DEFAULT (SYSUTCDATETIME()),

        CONSTRAINT [PK_workspace_members]
            PRIMARY KEY CLUSTERED ([workspace_id], [user_id]),
        CONSTRAINT [FK_workspace_members_workspace]
            FOREIGN KEY ([workspace_id]) REFERENCES [dbo].[workspaces] ([id])
            ON DELETE CASCADE,
        CONSTRAINT [FK_workspace_members_user]
            FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id])
            ON DELETE NO ACTION,
        CONSTRAINT [CK_workspace_members_role]
            CHECK ([role] IN ('OWNER', 'MEMBER'))
    );
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE [name] = N'IX_workspace_members_user_id'
      AND [object_id] = OBJECT_ID(N'dbo.workspace_members')
)
BEGIN
    CREATE NONCLUSTERED INDEX [IX_workspace_members_user_id]
        ON [dbo].[workspace_members] ([user_id], [workspace_id]);
END;
GO

IF OBJECT_ID(N'dbo.projects', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[projects]
    (
        [id] bigint IDENTITY(1, 1) NOT NULL,
        [workspace_id] bigint NOT NULL,
        [name] nvarchar(120) NOT NULL,
        [description] nvarchar(1000) NULL,
        [created_by_id] bigint NOT NULL,
        [created_at] datetimeoffset(6) NOT NULL
            CONSTRAINT [DF_projects_created_at] DEFAULT (SYSUTCDATETIME()),
        [updated_at] datetimeoffset(6) NOT NULL
            CONSTRAINT [DF_projects_updated_at] DEFAULT (SYSUTCDATETIME()),

        CONSTRAINT [PK_projects] PRIMARY KEY CLUSTERED ([id]),
        CONSTRAINT [FK_projects_workspace]
            FOREIGN KEY ([workspace_id]) REFERENCES [dbo].[workspaces] ([id])
            ON DELETE CASCADE,
        CONSTRAINT [FK_projects_created_by]
            FOREIGN KEY ([created_by_id]) REFERENCES [dbo].[users] ([id])
            ON DELETE NO ACTION,
        CONSTRAINT [CK_projects_name_not_blank]
            CHECK (LEN(LTRIM(RTRIM([name]))) > 0)
    );
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE [name] = N'IX_projects_workspace_id'
      AND [object_id] = OBJECT_ID(N'dbo.projects')
)
BEGIN
    CREATE NONCLUSTERED INDEX [IX_projects_workspace_id]
        ON [dbo].[projects] ([workspace_id], [id]);
END;
GO

IF OBJECT_ID(N'dbo.tasks', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[tasks]
    (
        [id] bigint IDENTITY(1, 1) NOT NULL,
        [project_id] bigint NOT NULL,
        [title] nvarchar(200) NOT NULL,
        [description] nvarchar(4000) NULL,
        [status] varchar(20) NOT NULL,
        [priority] varchar(20) NOT NULL,
        [position] int NOT NULL
            CONSTRAINT [DF_tasks_position] DEFAULT (0),
        [assignee_id] bigint NULL,
        [created_by_id] bigint NOT NULL,
        [due_at] datetimeoffset(6) NULL,
        [version] bigint NOT NULL
            CONSTRAINT [DF_tasks_version] DEFAULT (0),
        [created_at] datetimeoffset(6) NOT NULL
            CONSTRAINT [DF_tasks_created_at] DEFAULT (SYSUTCDATETIME()),
        [updated_at] datetimeoffset(6) NOT NULL
            CONSTRAINT [DF_tasks_updated_at] DEFAULT (SYSUTCDATETIME()),

        CONSTRAINT [PK_tasks] PRIMARY KEY CLUSTERED ([id]),
        CONSTRAINT [FK_tasks_project]
            FOREIGN KEY ([project_id]) REFERENCES [dbo].[projects] ([id])
            ON DELETE CASCADE,
        CONSTRAINT [FK_tasks_assignee]
            FOREIGN KEY ([assignee_id]) REFERENCES [dbo].[users] ([id])
            ON DELETE NO ACTION,
        CONSTRAINT [FK_tasks_created_by]
            FOREIGN KEY ([created_by_id]) REFERENCES [dbo].[users] ([id])
            ON DELETE NO ACTION,
        CONSTRAINT [CK_tasks_title_not_blank]
            CHECK (LEN(LTRIM(RTRIM([title]))) > 0),
        CONSTRAINT [CK_tasks_status]
            CHECK ([status] IN ('TODO', 'IN_PROGRESS', 'DONE')),
        CONSTRAINT [CK_tasks_priority]
            CHECK ([priority] IN ('LOW', 'MEDIUM', 'HIGH')),
        CONSTRAINT [CK_tasks_position_non_negative]
            CHECK ([position] >= 0)
    );
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE [name] = N'IX_tasks_project_status_position'
      AND [object_id] = OBJECT_ID(N'dbo.tasks')
)
BEGIN
    CREATE NONCLUSTERED INDEX [IX_tasks_project_status_position]
        ON [dbo].[tasks] ([project_id], [status], [position], [id]);
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE [name] = N'IX_tasks_assignee_id'
      AND [object_id] = OBJECT_ID(N'dbo.tasks')
)
BEGIN
    CREATE NONCLUSTERED INDEX [IX_tasks_assignee_id]
        ON [dbo].[tasks] ([assignee_id], [status]);
END;
GO

IF OBJECT_ID(N'dbo.comments', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[comments]
    (
        [id] bigint IDENTITY(1, 1) NOT NULL,
        [task_id] bigint NOT NULL,
        [author_id] bigint NOT NULL,
        [content] nvarchar(2000) NOT NULL,
        [created_at] datetimeoffset(6) NOT NULL
            CONSTRAINT [DF_comments_created_at] DEFAULT (SYSUTCDATETIME()),
        [updated_at] datetimeoffset(6) NOT NULL
            CONSTRAINT [DF_comments_updated_at] DEFAULT (SYSUTCDATETIME()),

        CONSTRAINT [PK_comments] PRIMARY KEY CLUSTERED ([id]),
        CONSTRAINT [FK_comments_task]
            FOREIGN KEY ([task_id]) REFERENCES [dbo].[tasks] ([id])
            ON DELETE CASCADE,
        CONSTRAINT [FK_comments_author]
            FOREIGN KEY ([author_id]) REFERENCES [dbo].[users] ([id])
            ON DELETE NO ACTION,
        CONSTRAINT [CK_comments_content_not_blank]
            CHECK (LEN(LTRIM(RTRIM([content]))) > 0)
    );
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE [name] = N'IX_comments_task_id'
      AND [object_id] = OBJECT_ID(N'dbo.comments')
)
BEGIN
    CREATE NONCLUSTERED INDEX [IX_comments_task_id]
        ON [dbo].[comments] ([task_id], [id]);
END;
GO

SELECT
    DB_NAME() AS database_name,
    SUSER_SNAME() AS executed_by,
    N'$(AppLogin)' AS application_login;
GO
