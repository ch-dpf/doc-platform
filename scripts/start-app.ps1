param(
    [int]$Port = 8088,
    [string]$Profile = "dev",
    [string]$MavenRepo = ".m2/repository",
    [switch]$SkipPackage
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not $SkipPackage) {
    mvn -q "-Dmaven.repo.local=$MavenRepo" -DskipTests package
}

$jar = Join-Path $root "knowbase-app\target\knowbase-app-1.0.0-SNAPSHOT.jar"
$javaArgs = @(
    "-jar", $jar,
    "--server.port=$Port"
)
if ($Profile) {
    $javaArgs += "--spring.profiles.active=$Profile"
}

Write-Host "Starting KnowBase on port $Port (profile: $Profile)" -ForegroundColor Cyan
java @javaArgs
