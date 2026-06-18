param(
    [int]$Port = 8080,
    [string]$MavenRepo = ".m2/repository",
    [switch]$SkipPackage
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not $SkipPackage) {
    mvn -q "-Dmaven.repo.local=$MavenRepo" -DskipTests package
}

java -jar "$root\knowbase-app\target\knowbase-app-1.0.0-SNAPSHOT.jar" "--server.port=$Port"
