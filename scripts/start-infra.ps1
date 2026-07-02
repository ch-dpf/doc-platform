param(
    [switch]$WithMinio,
    [switch]$WithOllama,
    [int]$WaitSeconds = 60
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$services = @("postgres")
if ($WithMinio) { $services += "minio" }
if ($WithOllama) { $services += "ollama" }

Write-Host "Starting Docker services: $($services -join ', ')" -ForegroundColor Cyan
docker compose up -d @services
if ($LASTEXITCODE -ne 0) {
    if ((docker ps -a --filter "name=knowbase-postgres" --format "{{.Names}}") -eq "knowbase-postgres") {
        Write-Host "Retrying after removing stale knowbase-postgres container..." -ForegroundColor Yellow
        docker rm -f knowbase-postgres | Out-Null
        docker compose up -d @services
    }
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up failed"
    }
}

$deadline = (Get-Date).AddSeconds($WaitSeconds)
while ((Get-Date) -lt $deadline) {
    $status = docker inspect -f "{{.State.Health.Status}}" knowbase-postgres 2>$null
    if ($status -eq "healthy") {
        Write-Host "PostgreSQL is healthy on localhost:5433" -ForegroundColor Green
        exit 0
    }
    Start-Sleep -Seconds 2
}

Write-Host "PostgreSQL not healthy yet; check: docker logs knowbase-postgres" -ForegroundColor Yellow
exit 1
