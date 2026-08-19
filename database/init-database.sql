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

SELECT
    DB_NAME() AS database_name,
    SUSER_SNAME() AS executed_by,
    N'$(AppLogin)' AS application_login;
GO

