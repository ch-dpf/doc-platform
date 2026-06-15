# Start knowbase-app (run infra-check first)
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$jar = "$root\knowbase-app\target\knowbase-app-1.0.0-SNAPSHOT.jar"

if (-not (Test-Path $jar)) {
    Write-Host "Missing $jar - run .\scripts\build.ps1"
    exit 1
}

Write-Host "Starting knowbase-app (8010) ..."
# 与 JDBC client_encoding=UTF8 配合，避免 Windows 默认 GBK 导致中文乱码
Start-Process java -ArgumentList "-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8", "-jar", $jar -WorkingDirectory $root -WindowStyle Normal

Write-Host ""
Write-Host "Service starting in new window."
Write-Host "  API / Knife4j: http://localhost:8010/doc.html"
Write-Host "  Frontend:      cd frontend\knowbase-ui && npm run dev  -> http://localhost:5173 (proxies /api to 8010)"
Write-Host "  E2E test:      .\scripts\e2e-test.ps1"
