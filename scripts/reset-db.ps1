<#
.SYNOPSIS
  重置 doc-platform 数据库为单 schema public 并重新建表。

.DESCRIPTION
  默认在运行中的 Docker Postgres 容器内执行 SQL（删除旧双 schema、清空 public 表、执行 init.sql）。
  使用 -RecreateContainer 可删除 Postgres 容器后重建（空库自动跑 docker-entrypoint-initdb.d/init.sql）。

.PARAMETER RecreateContainer
  删除并重建 postgres 服务容器（适合 init 未执行或希望完全空库的场景）。

.PARAMETER SkipConfirm
  跳过确认提示。

.PARAMETER UseLocalPsql
  使用本机 psql（见 infra-check.ps1 路径），不通过 Docker。

.EXAMPLE
  .\scripts\reset-db.ps1

.EXAMPLE
  .\scripts\reset-db.ps1 -RecreateContainer -SkipConfirm
#>
[CmdletBinding()]
param(
    [switch] $RecreateContainer,
    [switch] $SkipConfirm,
    [switch] $UseLocalPsql,
    [string] $DbHost = "localhost",
    [int] $DbPort = 5432,
    [string] $DbUser = "docplatform",
    [string] $DbName = "docplatform",
    [string] $DbPassword = "docplatform",
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
    throw "RecreateContainer 需要 Docker。请安装 Docker Desktop，或去掉 -RecreateContainer 并使用本机 PostgreSQL（-UseLocalPsql）。"
}

if (-not $UseLocalPsql -and -not $dockerAvailable) {
    if ($resolvedPsql) {
        Write-Host "未检测到 Docker，将使用本机 psql: $resolvedPsql"
        $UseLocalPsql = $true
        $LocalPsql = $resolvedPsql
    } else {
        throw "未找到 docker 命令，也未找到本机 psql。请安装 Docker 后运行 start-infra.ps1，或安装 PostgreSQL 后使用 -UseLocalPsql。"
    }
}

if ($UseLocalPsql) {
    if (-not $resolvedPsql) {
        throw "未找到本机 psql（已尝试: $LocalPsql 以及 PATH 中的 psql）。"
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
        "重建 Postgres 容器并初始化"
    } elseif ($UseLocalPsql) {
        "本机 psql 就地清空表并执行 init.sql"
    } else {
        "Docker 就地清空表并执行 init.sql"
    }
    Write-Host "将重置数据库 [$DbName]（$mode）。"
    Write-Host "Warning: drops vector_library, doc_metadata, document_chunk, etc."
    $answer = Read-Host "输入 yes 继续"
    if ($answer -ne "yes") {
        Write-Host "已取消。"
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
    throw "Postgres 在 ${MaxSeconds}s 内未就绪，请检查 docker compose logs postgres"
}

function Invoke-SqlFile-Docker {
    param([string] $Path)
    $content = Get-Content -Path $Path -Raw -Encoding UTF8
    $content | docker compose exec -T -e PGCLIENTENCODING=UTF8 postgres psql -v ON_ERROR_STOP=1 -U $DbUser -d $DbName
    if ($LASTEXITCODE -ne 0) {
        throw "psql failed for $(Split-Path $Path -Leaf) (exit $LASTEXITCODE)"
    }
}

function Invoke-SqlFile-Local {
    param([string] $Path)
    # Windows: 用 UTF-8 BOM 临时文件 + psql -f，比管道更稳（避免中文种子 INSERT 被跳过）
    $env:PGPASSWORD = $DbPassword
    $env:PGCLIENTENCODING = 'UTF8'
    $content = Get-Content -Path $Path -Raw -Encoding UTF8
    $tempFile = [System.IO.Path]::Combine(
        [System.IO.Path]::GetTempPath(),
        "docplatform-" + [Guid]::NewGuid().ToString("N") + ".sql")
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
    Write-Host "Starting postgres (init.sql runs on empty data directory)..."
    docker compose up -d postgres
    Wait-PostgresReady
    Write-Host "Database reset via container recreate completed."
    exit 0
}

if ($UseLocalPsql) {
    Write-Host ('Resetting via local psql ({0}:{1})...' -f $DbHost, $DbPort)
    foreach ($f in $files) {
        Write-Host "  -> $(Split-Path $f -Leaf)"
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
        Write-Host "  -> $(Split-Path $f -Leaf)"
        Invoke-SqlFile-Docker -Path $f
    }
}

Write-Host ""
Write-Host "Done. Tables in schema public:"
Write-Host "  vector_library, doc_metadata, document_chunk, ..."
Write-Host ""
Write-Host "UTF-8: re-run this script if Chinese was garbled; restart backend via start-services.ps1 (-Dfile.encoding=UTF-8)."
Write-Host "Check encoding: .\scripts\check-db-encoding.ps1 -UseLocalPsql"
Write-Host "Restart doc-platform-service if it is running, then re-ingest documents."
