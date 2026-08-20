[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $repositoryRoot '.env.compose'

if (Test-Path -LiteralPath $envPath) {
    throw '.env.compose already exists. Remove it explicitly only if you want to replace the current Docker secrets.'
}

$generator = [Security.Cryptography.RandomNumberGenerator]::Create()

function New-RandomBase64 {
    param([int]$ByteCount)

    $bytes = New-Object byte[] $ByteCount
    $generator.GetBytes($bytes)
    return [Convert]::ToBase64String($bytes)
}

try {
    $saPassword = 'Sa1!' + (New-RandomBase64 -ByteCount 24)
    $appPassword = 'Db1!' + (New-RandomBase64 -ByteCount 24)
    $jwtSecret = New-RandomBase64 -ByteCount 32

    $lines = @(
        "MSSQL_SA_PASSWORD=$saPassword"
        "DB_PASSWORD=$appPassword"
        "JWT_SECRET=$jwtSecret"
        'JWT_ISSUER=nextask-api'
        'JWT_ACCESS_TOKEN_TTL=PT1H'
        'OPENAPI_ENABLED=false'
        'APP_PORT=8080'
    )

    $encoding = New-Object System.Text.UTF8Encoding($false)
    [IO.File]::WriteAllLines($envPath, $lines, $encoding)
} finally {
    $generator.Dispose()
    Remove-Variable saPassword, appPassword, jwtSecret -ErrorAction SilentlyContinue
}

Write-Host '.env.compose created with private random secrets.'
Write-Host 'Keep this file local and do not publish it.'
