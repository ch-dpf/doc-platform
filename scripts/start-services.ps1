# Start doc-platform-service (run infra-check first)
$ErrorActionPreference = "Stop"
$root = "D:\workspace\doc-platform"
$jar = "$root\doc-platform-service\target\doc-platform-service-1.0.0-SNAPSHOT.jar"

if (-not (Test-Path $jar)) {
    Write-Host "Missing $jar - run .\scripts\build.ps1"
    exit 1
}

Write-Host "Starting doc-platform-service (8080) ..."
# 与 JDBC client_encoding=UTF8 配合，避免 Windows 默认 GBK 导致中文乱码
Start-Process java -ArgumentList "-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8", "-jar", $jar -WorkingDirectory $root -WindowStyle Normal

Write-Host ""
Write-Host "Service starting in new window."
Write-Host "  API / Knife4j: http://localhost:8080/doc.html"
Write-Host "  Frontend:      cd frontend\doc-platform-ui && npm run dev  -> http://localhost:5173"
Write-Host "  E2E test:      .\scripts\e2e-test.ps1"
