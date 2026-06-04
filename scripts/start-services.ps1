# Start doc-platform Java services (run infra-check first)
$ErrorActionPreference = "Stop"
$root = "D:\workspace\doc-platform"
$ingestJar = "$root\doc-ingest-service\target\doc-ingest-service-1.0.0-SNAPSHOT.jar"
$vectorJar = "$root\vector-index-service\target\vector-index-service-1.0.0-SNAPSHOT.jar"

if (-not (Test-Path $ingestJar)) {
    Write-Host "Missing $ingestJar - run .\scripts\build.ps1"
    exit 1
}
if (-not (Test-Path $vectorJar)) {
    Write-Host "Missing $vectorJar - run .\scripts\build.ps1"
    exit 1
}

Write-Host "Starting doc-ingest-service (8081) ..."
Start-Process java -ArgumentList "-jar", $ingestJar -WorkingDirectory $root -WindowStyle Normal

Start-Sleep -Seconds 5

Write-Host "Starting vector-index-service (8082) ..."
Start-Process java -ArgumentList "-jar", $vectorJar -WorkingDirectory $root -WindowStyle Normal

Write-Host ""
Write-Host "Services starting in new windows."
Write-Host "  Ingest API: http://localhost:8081/doc.html"
Write-Host "  Vector API: http://localhost:8082/doc.html"
Write-Host "  E2E test:   D:\workspace\doc-platform\scripts\e2e-test.ps1"
