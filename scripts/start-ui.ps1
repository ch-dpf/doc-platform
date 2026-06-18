param(
    [int]$Port = 5173,
    [string]$NpmCache = "..\..\.npm-cache",
    [switch]$SkipInstall
)

$ErrorActionPreference = "Stop"
$uiRoot = Join-Path (Split-Path -Parent $PSScriptRoot) "frontend\knowbase-ui"
Set-Location $uiRoot

if (-not $SkipInstall) {
    npm install --cache $NpmCache
}

npm run dev -- --host 0.0.0.0 --port $Port
