param(
    [int]$Port = 8088,
    [string]$Profile = "dev",
    [string]$MavenRepo = ".m2/repository",
    [switch]$SkipInfra,
    [switch]$SkipPackage
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not $SkipInfra) {
    & "$PSScriptRoot\start-infra.ps1"
}

if (-not $SkipPackage) {
    mvn -q "-Dmaven.repo.local=$MavenRepo" -DskipTests package
}

$jar = Join-Path $root "knowbase-app\target\knowbase-app-1.0.0-SNAPSHOT.jar"
$args = @(
    "-jar", $jar,
    "--server.port=$Port",
    "--spring.profiles.active=$Profile"
)

Write-Host "Starting backend: profile=$Profile port=$Port" -ForegroundColor Cyan
java @args
