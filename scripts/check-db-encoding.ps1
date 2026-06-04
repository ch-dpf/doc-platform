<#
.SYNOPSIS
  Check PostgreSQL server/client encoding (UTF-8 troubleshooting).

.EXAMPLE
  .\scripts\check-db-encoding.ps1
  .\scripts\check-db-encoding.ps1 -UseLocalPsql
#>
[CmdletBinding()]
param(
    [switch] $UseLocalPsql,
    [string] $DbHost = "localhost",
    [int] $DbPort = 5432,
    [string] $DbUser = "docplatform",
    [string] $DbName = "docplatform",
    [string] $DbPassword = "docplatform",
    [string] $LocalPsql = "D:\software\PostgreSQL\pgsql\bin\psql.exe"
)

$ErrorActionPreference = "Stop"
$env:PGPASSWORD = $DbPassword
$env:PGCLIENTENCODING = "UTF8"

$sql = @"
SELECT current_database() AS db,
       pg_encoding_to_char(encoding) AS server_encoding
FROM pg_database WHERE datname = current_database();
SHOW client_encoding;
SELECT library_id, name, description
FROM vector_library
ORDER BY created_at
LIMIT 3;
"@

function Invoke-Query-Docker {
    $sql | docker compose exec -T -e PGCLIENTENCODING=UTF8 postgres psql -U $DbUser -d $DbName
}

function Invoke-Query-Local {
    $psql = if (Test-Path $LocalPsql) { $LocalPsql } else { (Get-Command psql -ErrorAction SilentlyContinue).Source }
    if (-not $psql) { throw "psql not found" }
    $sql | & $psql -h $DbHost -p $DbPort -U $DbUser -d $DbName
}

Write-Host "=== PostgreSQL encoding check ===" -ForegroundColor Cyan
if ($UseLocalPsql) {
    Invoke-Query-Local
} else {
    Invoke-Query-Docker
}
Write-Host ""
Write-Host "Expected: server_encoding=UTF8, client_encoding=UTF8, Chinese text OK in vector_library." -ForegroundColor Green
Write-Host "If garbled, run: .\scripts\reset-db.ps1" -ForegroundColor Yellow
Write-Host "Docker fresh DB: .\scripts\reset-db.ps1 -RecreateContainer -SkipConfirm" -ForegroundColor Yellow
