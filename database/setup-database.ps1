[CmdletBinding()]
param(
    [string]$ServerInstance = '.\SQLEXPRESS',
    [string]$EnvironmentFile
)

$ErrorActionPreference = 'Stop'
$scriptDirectory = Split-Path -Parent $PSCommandPath

if ([string]::IsNullOrWhiteSpace($EnvironmentFile)) {
    $EnvironmentFile = Join-Path $scriptDirectory '..\.env'
}

$sqlcmd = Get-Command sqlcmd -ErrorAction SilentlyContinue
if (-not $sqlcmd) {
    throw 'sqlcmd was not found. Install SQL Server command-line utilities or run init-database.sql in SSMS.'
}

if (-not (Test-Path -LiteralPath $EnvironmentFile)) {
    throw "Environment file not found: $EnvironmentFile. Copy .env.example to .env first."
}

$passwordEntry = Get-Content -LiteralPath $EnvironmentFile |
    Where-Object { $_ -match '^DB_PASSWORD=' } |
    Select-Object -First 1

if (-not $passwordEntry) {
    throw 'DB_PASSWORD is missing from the environment file.'
}

$databasePassword = $passwordEntry.Substring('DB_PASSWORD='.Length)
if ([string]::IsNullOrWhiteSpace($databasePassword)) {
    throw 'DB_PASSWORD cannot be empty.'
}

$sqlScript = Join-Path $scriptDirectory 'init-database.sql'

& $sqlcmd.Source `
    -S $ServerInstance `
    -E `
    -C `
    -b `
    -v "DB_PASSWORD=$databasePassword" `
    -i $sqlScript

if ($LASTEXITCODE -ne 0) {
    throw 'Database setup failed. Review the SQL Server authentication mode and the error above.'
}

Write-Host 'NexTask database, schema, and application login are ready.' -ForegroundColor Green
