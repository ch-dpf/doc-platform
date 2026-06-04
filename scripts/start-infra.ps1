# Start infrastructure for doc-platform
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
docker compose up -d
Write-Host "Waiting for Postgres..."
Start-Sleep -Seconds 8
Write-Host "Pull Ollama embedding model (first run may take several minutes)..."
docker compose exec ollama ollama pull nomic-embed-text
Write-Host "Pull Ollama chat model for RAG..."
docker compose exec ollama ollama pull llama3.2
Write-Host "Infrastructure ready."
