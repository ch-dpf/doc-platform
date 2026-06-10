# Check knowbase infrastructure (no Kafka)
$ErrorActionPreference = "Continue"
$ok = $true

function Test-Step {
    param([string]$Name, [scriptblock]$Block)
    Write-Host -NoNewline "$Name ... "
    try {
        & $Block
        if ($LASTEXITCODE -ne 0 -and $null -ne $LASTEXITCODE) { throw "exit $LASTEXITCODE" }
        Write-Host "OK"
    } catch {
        Write-Host "FAIL ($($_.Exception.Message))"
        $script:ok = $false
    }
}

Test-Step "PostgreSQL" {
    $env:PGPASSWORD = "knowbase"
    & "D:\software\PostgreSQL\pgsql\bin\psql.exe" -U knowbase -d knowbase -tAc "SELECT 1;" | Out-Null
}

Test-Step "pgvector" {
    $env:PGPASSWORD = "knowbase"
    $v = & "D:\software\PostgreSQL\pgsql\bin\psql.exe" -U knowbase -d knowbase -tAc "SELECT extname FROM pg_extension WHERE extname='vector';"
    if ($v.Trim() -ne "vector") { throw "extension missing" }
}

Test-Step "MinIO" {
    $r = Invoke-WebRequest -Uri "http://localhost:9000/minio/health/live" -UseBasicParsing -TimeoutSec 5
    if ($r.StatusCode -ne 200) { throw "status $($r.StatusCode)" }
}

Test-Step "Ollama embedding" {
    $tags = Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -TimeoutSec 5
    if (-not ($tags.models.name -match "nomic-embed-text")) { throw "nomic-embed-text missing" }
}

Test-Step "Ollama chat (RAG)" {
    $tags = Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -TimeoutSec 5
    if (-not ($tags.models.name -match "llama3.2")) { throw "llama3.2 missing (ollama pull llama3.2)" }
}

$repoRoot = Split-Path $PSScriptRoot -Parent
$tessDir = Join-Path $repoRoot "infra\tesseract\tessdata"
$chiSim = Join-Path $tessDir "chi_sim.traineddata"
if (Test-Path $chiSim) {
    Test-Step "OCR tessdata (chi_sim)" {
        if ((Get-Item $chiSim).Length -lt 40MB) { throw "chi_sim.traineddata too small" }
    }
} else {
    Write-Host "OCR tessdata (chi_sim) ... SKIP (run .\scripts\setup-tesseract.ps1 if OCR needed)"
}

Write-Host ""
if ($ok) {
    Write-Host "All checks passed. Start knowbase-service next."
    exit 0
} else {
    Write-Host "Some checks failed. Fix infra before starting Java."
    exit 1
}
