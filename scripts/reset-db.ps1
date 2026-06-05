<#
.SYNOPSIS
  Reset knowbase database (single schema public) and re-run init.sql.

.DESCRIPTION
  Default: run SQL inside Docker Postgres container.
  -RecreateContainer: recreate postgres container (empty data dir runs init via docker-entrypoint).
  -UseLocalPsql -BootstrapLocal: drop docplatform/knowbase on local PostgreSQL, recreate knowbase role/db, then init.

.PARAMETER RecreateContainer
  Remove and recreate postgres service container.

.PARAMETER SkipConfirm
  Skip interactive confirmation.

.PARAMETER UseLocalPsql
  Use local psql instead of Docker.

.PARAMETER BootstrapLocal
  Local only: run bootstrap-knowbase-local.sql as superuser before init (destroys old docplatform/knowbase data).

.PARAMETER AdminUser
  PostgreSQL superuser for bootstrap, default postgres.

.PARAMETER AdminPassword
  Superuser password; falls back to env PGPASSWORD_ADMIN.

.EXAMPLE
  .\scripts\reset-db.ps1 -UseLocalPsql -BootstrapLocal -AdminPassword "123456" -SkipConfirm
#>
[CmdletBinding()]
param(
    [switch] $RecreateContainer,
    [switch] $SkipConfirm,
    [switch] $UseLocalPsql,
    [switch] $BootstrapLocal,
    [string] $AdminUser = "postgres",
    [string] $AdminPassword = "",
    [string] $DbHost = "localhost",
    [int] $DbPort = 5432,
    [string] $DbUser = "knowbase",
    [string] $DbName = "knowbase",
    [string] $DbPassword = "knowbase",
    [string] $LocalPsql = "D:\software\PostgreSQL\pgsql\bin\psql.exe"
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $Root

function Test-DockerComposeAvailable {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        return $false
    }
    docker compose version 2>$null | Out-Null
    return $LASTEXITCODE -eq 0
}

function Resolve-LocalPsqlPath {
    param([string] $Preferred)
    if ($Preferred -and (Test-Path $Preferred)) {
        return (Resolve-Path $Preferred).Path
    }
    $cmd = Get-Command psql -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }
    return $null
}

$dockerAvailable = Test-DockerComposeAvailable
$resolvedPsql = Resolve-LocalPsqlPath -Preferred $LocalPsql

if ($RecreateContainer -and -not $dockerAvailable) {
    throw "RecreateContainer requires Docker. Use -UseLocalPsql for local PostgreSQL instead."
}

if ($BootstrapLocal -and -not $UseLocalPsql) {
    $UseLocalPsql = $true
}

if (-not $UseLocalPsql -and -not $dockerAvailable) {
    if ($resolvedPsql) {
        Write-Host "Docker not found, using local psql: $resolvedPsql"
        $UseLocalPsql = $true
        $LocalPsql = $resolvedPsql
    } else {
        throw "Neither docker nor local psql found."
    }
}

if ($UseLocalPsql) {
    if (-not $resolvedPsql) {
        throw "Local psql not found (tried: $LocalPsql and PATH)."
    }
    $LocalPsql = $resolvedPsql
}

$sqlDir = Join-Path $Root "infra\postgres"
$files = @(
    (Join-Path $sqlDir "recreate-single-schema.sql"),
    (Join-Path $sqlDir "drop-public-tables.sql"),
    (Join-Path $sqlDir "init.sql")
)
foreach ($f in $files) {
    if (-not (Test-Path $f)) {
        throw "SQL file not found: $f"
    }
}

if (-not $SkipConfirm) {
    $mode = if ($RecreateContainer) {
        "recreate postgres container"
    } elseif ($BootstrapLocal -and $UseLocalPsql) {
        "local bootstrap + init.sql (drops docplatform/knowbase)"
    } elseif ($UseLocalPsql) {
        "local psql in-place reset + init.sql"
    } else {
        "docker in-place reset + init.sql"
    }
    Write-Host "Will reset database [$DbName] ($mode)."
    Write-Host "Warning: drops vector_library, doc_metadata, document_chunk, etc."
    $answer = Read-Host "Type yes to continue"
    if ($answer -ne "yes") {
        Write-Host "Cancelled."
        exit 0
    }
}

function Wait-PostgresReady {
    param([int] $MaxSeconds = 60)
    $deadline = (Get-Date).AddSeconds($MaxSeconds)
    while ((Get-Date) -lt $deadline) {
        docker compose exec -T postgres pg_isready -U $DbUser -d $DbName 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "Postgres not ready within ${MaxSeconds}s. Check: docker compose logs postgres"
}

function Invoke-SqlFile-Docker {
    param([string] $Path)
    $content = Get-Content -Path $Path -Raw -Encoding UTF8
    $content | docker compose exec -T -e PGCLIENTENCODING=UTF8 postgres psql -v ON_ERROR_STOP=1 -U $DbUser -d $DbName
    if ($LASTEXITCODE -ne 0) {
        throw "psql failed for $(Split-Path $Path -Leaf) (exit $LASTEXITCODE)"
    }
}

function Test-LocalDbConnection {
    param(
        [string] $User,
        [string] $Password,
        [string] $Database
    )
    $env:PGPASSWORD = $Password
    $env:PGCLIENTENCODING = "UTF8"
    & $LocalPsql -h $DbHost -p $DbPort -U $User -d $Database -tAc "SELECT 1;" 2>$null | Out-Null
    return $LASTEXITCODE -eq 0
}

function Invoke-BootstrapLocal {
    param([string] $Password)
    $bootstrap = Join-Path $sqlDir "bootstrap-knowbase-local.sql"
    if (-not (Test-Path $bootstrap)) {
        throw "SQL file not found: $bootstrap"
    }
    Write-Host "Bootstrapping local database (drops docplatform/knowbase)..."
    $env:PGPASSWORD = $Password
    $env:PGCLIENTENCODING = "UTF8"
    $content = Get-Content -Path $bootstrap -Raw -Encoding UTF8
    $tempFile = [System.IO.Path]::Combine(
        [System.IO.Path]::GetTempPath(),
        "knowbase-bootstrap-" + [Guid]::NewGuid().ToString("N") + ".sql")
    try {
        $utf8Bom = New-Object System.Text.UTF8Encoding $true
        [System.IO.File]::WriteAllText($tempFile, $content, $utf8Bom)
        & $LocalPsql -h $DbHost -p $DbPort -U $AdminUser -d postgres -v ON_ERROR_STOP=1 -f $tempFile
        if ($LASTEXITCODE -ne 0) {
            throw "bootstrap-knowbase-local.sql failed (exit $LASTEXITCODE)"
        }
    } finally {
        if (Test-Path $tempFile) {
            Remove-Item -LiteralPath $tempFile -Force -ErrorAction SilentlyContinue
        }
    }
}

function Invoke-SqlFile-Local {
    param([string] $Path)
    $env:PGPASSWORD = $DbPassword
    $env:PGCLIENTENCODING = "UTF8"
    $content = Get-Content -Path $Path -Raw -Encoding UTF8
    $tempFile = [System.IO.Path]::Combine(
        [System.IO.Path]::GetTempPath(),
        "knowbase-" + [Guid]::NewGuid().ToString("N") + ".sql")
    try {
        $utf8Bom = New-Object System.Text.UTF8Encoding $true
        [System.IO.File]::WriteAllText($tempFile, $content, $utf8Bom)
        & $LocalPsql -h $DbHost -p $DbPort -U $DbUser -d $DbName -v ON_ERROR_STOP=1 -f $tempFile
        if ($LASTEXITCODE -ne 0) {
            throw "psql failed for $(Split-Path $Path -Leaf) (exit $LASTEXITCODE)"
        }
    } finally {
        if (Test-Path $tempFile) {
            Remove-Item -LiteralPath $tempFile -Force -ErrorAction SilentlyContinue
        }
    }
}

if ($RecreateContainer) {
    Write-Host "Stopping and removing postgres container..."
    docker compose stop postgres 2>$null | Out-Null
    docker compose rm -f postgres 2>$null | Out-Null
    Write-Host "Starting postgres (init.sql on empty data directory)..."
    docker compose up -d postgres
    Wait-PostgresReady
    Write-Host "Database reset via container recreate completed."
    exit 0
}

if ($UseLocalPsql) {
    if ($BootstrapLocal) {
        $adminPass = $AdminPassword
        if (-not $adminPass) {
            $adminPass = $env:PGPASSWORD_ADMIN
        }
        if (-not $adminPass) {
            throw "BootstrapLocal requires -AdminPassword or env PGPASSWORD_ADMIN."
        }
        Invoke-BootstrapLocal -Password $adminPass
    } elseif (-not (Test-LocalDbConnection -User $DbUser -Password $DbPassword -Database $DbName)) {
        throw "Cannot connect to [$DbName] as $DbUser. Use -BootstrapLocal when upgrading from docplatform."
    }

    Write-Host ("Resetting via local psql ({0}:{1})..." -f $DbHost, $DbPort)
    foreach ($f in $files) {
        Write-Host ("  -> {0}" -f (Split-Path $f -Leaf))
        Invoke-SqlFile-Local -Path $f
    }
} else {
    $running = docker compose ps postgres --status running -q 2>$null
    if (-not $running) {
        Write-Host "Postgres container not running, starting..."
        docker compose up -d postgres
        Wait-PostgresReady
    }
    Write-Host "Resetting via Docker postgres..."
    foreach ($f in $files) {
        Write-Host ("  -> {0}" -f (Split-Path $f -Leaf))
        Invoke-SqlFile-Docker -Path $f
    }
}

Write-Host ""
Write-Host "Done. Tables: vector_library, doc_metadata, document_chunk, ..."
Write-Host "Restart knowbase-service, then re-ingest documents."
