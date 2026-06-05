# Build knowbase (local Maven, no global PATH required)
param(
    [switch]$Test,
    [switch]$NoClean
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path $PSScriptRoot -Parent
$MavenHome = if ($env:MAVEN_HOME) { $env:MAVEN_HOME } else { "D:\software\maven\apache-maven-3.9.11" }
$Mvn = Join-Path $MavenHome "bin\mvn.cmd"

if (-not (Test-Path $Mvn)) {
    throw "Maven not found at $Mvn. Set MAVEN_HOME or install Maven under D:\software\maven\"
}

Set-Location $ProjectRoot
$goals = @()
if (-not $NoClean) { $goals += "clean" }
$goals += "package"
if (-not $Test) { $goals += "-DskipTests" }

Write-Host "mvn $($goals -join ' ')"
& $Mvn @goals
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Build OK. JARs:"
Write-Host "  knowbase-service\target\knowbase-service-1.0.0-SNAPSHOT.jar"
