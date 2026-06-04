# Check doc-platform infrastructure before starting Java services
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
    $env:PGPASSWORD = "docplatform"
    & "D:\software\PostgreSQL\pgsql\bin\psql.exe" -U docplatform -d docplatform -tAc "SELECT 1;" | Out-Null
}

Test-Step "pgvector" {
    $env:PGPASSWORD = "docplatform"
    $v = & "D:\software\PostgreSQL\pgsql\bin\psql.exe" -U docplatform -d docplatform -tAc "SELECT extname FROM pg_extension WHERE extname='vector';"
    if ($v.Trim() -ne "vector") { throw "extension missing" }
}

Test-Step "Kafka" {
    & "D:\software\Kafka\bin\windows\kafka-broker-api-versions.bat" --bootstrap-server localhost:9092 2>$null | Out-Null
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

Write-Host ""
if ($ok) {
    Write-Host "All checks passed. Start Java services next."
    exit 0
} else {
    Write-Host "Some checks failed. Fix infra before starting Java."
    exit 1
}
