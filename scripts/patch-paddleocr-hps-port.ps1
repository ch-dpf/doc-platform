param(
    [Parameter(Mandatory = $true)]
    [string]$HpsDir,
    [int]$HostPort = 8888,
    [int]$ContainerPort = 8080
)

$ErrorActionPreference = "Stop"

function Resolve-HpsDir {
    param([string]$InputPath)
    $candidates = @(
        $InputPath,
        (Join-Path $PWD.Path $InputPath),
        "D:\workspace\PaddleOCR\deploy\paddleocr_vl_docker\hps",
        "D:\PaddleOCR\deploy\paddleocr_vl_docker\hps"
    ) | Select-Object -Unique

    foreach ($candidate in $candidates) {
        if (-not $candidate) { continue }
        $resolved = Resolve-Path -LiteralPath $candidate -ErrorAction SilentlyContinue
        if ($resolved -and (Test-Path (Join-Path $resolved "compose.yaml"))) {
            return $resolved.Path
        }
    }
    throw @"
compose.yaml not found under '$InputPath'.

Common causes:
  1) Wrong path — your clone is likely D:\workspace\PaddleOCR\..., not D:\PaddleOCR\...
  2) PaddleOCR repo not cloned yet

Try:
  .\scripts\patch-paddleocr-hps-port.ps1 -HpsDir D:\workspace\PaddleOCR\deploy\paddleocr_vl_docker\hps
"@
}

$HpsDir = Resolve-HpsDir -InputPath $HpsDir
$composeFile = Join-Path $HpsDir "compose.yaml"
if (-not (Test-Path $composeFile)) {
    $composeFile = Join-Path $HpsDir "docker-compose.yml"
}
if (-not (Test-Path $composeFile)) {
    throw "compose.yaml / docker-compose.yml not found under $HpsDir"
}

$content = Get-Content -Path $composeFile -Raw -Encoding UTF8
$mapping = "${HostPort}:${ContainerPort}"
if ($content -match "\b${HostPort}:${ContainerPort}\b") {
    Write-Host "Port mapping already set to $mapping in $composeFile"
    exit 0
}

$patterns = @(
    @{ Pattern = '(?m)^(\s*-\s*)8080:8080\s*$'; Replacement = "`${1}${mapping}" },
    @{ Pattern = '(?m)^(\s*-\s*")8080:8080("\s*)$'; Replacement = "`${1}${mapping}`${2}" }
)
$newContent = $content
foreach ($entry in $patterns) {
    $candidate = [regex]::Replace($newContent, $entry.Pattern, $entry.Replacement, 1)
    if ($candidate -ne $newContent) {
        $newContent = $candidate
        break
    }
}

if ($newContent -eq $content) {
    throw "Could not find paddleocr-vl-api ports entry '8080:8080' in $composeFile. Patch manually to '$mapping'."
}

Set-Content -Path $composeFile -Value $newContent -Encoding UTF8 -NoNewline
Write-Host "Patched $composeFile => paddleocr-vl-api ports: $mapping"
Write-Host "Next: cd `"$HpsDir`"; docker compose up -d"
Write-Host "Then: .\scripts\verify-paddleocr-hps.ps1 -HpsBaseUrl http://localhost:$HostPort"
