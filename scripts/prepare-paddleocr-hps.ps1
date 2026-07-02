param(
    [Parameter(Mandatory = $true)]
    [string]$HpsDir
)

$ErrorActionPreference = "Stop"

function Read-DotEnv {
    param([string]$Path)
    $values = @{}
    if (-not (Test-Path $Path)) {
        return $values
    }
    Get-Content -Path $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) {
            return
        }
        $key = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        $values[$key] = $value
    }
    return $values
}

function Set-DotEnvValue {
    param(
        [string]$Path,
        [string]$Key,
        [string]$Value
    )
    if (-not (Test-Path $Path)) {
        return
    }
    $lines = Get-Content -Path $Path
    $found = $false
    $updated = foreach ($line in $lines) {
        if ($line -match "^\s*$([regex]::Escape($Key))\s*=") {
            $found = $true
            "$Key=$Value"
        } else {
            $line
        }
    }
    if (-not $found) {
        $updated += "$Key=$Value"
    }
    Set-Content -Path $Path -Value $updated -Encoding UTF8
}

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
    throw "HPS directory not found. Pass -HpsDir to PaddleOCR/deploy/paddleocr_vl_docker/hps (your clone is likely D:\workspace\PaddleOCR\...)."
}

$HpsDir = Resolve-HpsDir -InputPath $HpsDir
$envFile = Join-Path $HpsDir ".env"
$envExample = Join-Path $HpsDir ".env.example"
if (-not (Test-Path $envFile)) {
    if (-not (Test-Path $envExample)) {
        throw ".env and .env.example not found under $HpsDir"
    }
    Copy-Item -Path $envExample -Destination $envFile
    Write-Host "Created $envFile from .env.example"
}

$envValues = Read-DotEnv -Path $envFile
$pipelineName = if ($envValues["HPS_PIPELINE_NAME"]) { $envValues["HPS_PIPELINE_NAME"] } else { "PaddleOCR-VL-1.6" }
$paddlexVersion = if ($envValues["HPS_PADDLEX_VERSION"]) { $envValues["HPS_PADDLEX_VERSION"] } else { "3.6" }
$sdkVersion = if ($envValues["HPS_SDK_VERSION"]) { $envValues["HPS_SDK_VERSION"] } else { "v$paddlexVersion" }
$sdkDirName = if ($envValues["HPS_SDK_DIR"]) { $envValues["HPS_SDK_DIR"] } else { "paddlex_hps_${pipelineName}_sdk" }
$vlmUrl = if ($envValues["HPS_VLM_URL"]) { $envValues["HPS_VLM_URL"] } else { "http://paddleocr-vlm-server:8080" }

$sdkArchive = "$sdkDirName.tar.gz"
$sdkUrl = "https://paddle-model-ecology.bj.bcebos.com/paddlex/PaddleX3.0/deploy/paddlex_hps/public/sdks/$sdkVersion/$sdkArchive"
$sdkRoot = Join-Path $HpsDir $sdkDirName
$pipelineConfig = Join-Path $sdkRoot "server/pipeline_config.yaml"
$archivePath = Join-Path $HpsDir $sdkArchive

Write-Host "Preparing high-stability serving SDK for $pipelineName"
Write-Host " HPS dir: $HpsDir"
Write-Host " SDK archive: $sdkArchive"

if (-not (Test-Path $pipelineConfig)) {
    if (-not (Test-Path $archivePath)) {
        Write-Host "Downloading $sdkUrl ..."
        Invoke-WebRequest -Uri $sdkUrl -OutFile $archivePath -UseBasicParsing
    } else {
        Write-Host "Using existing archive $archivePath"
    }

    Write-Host "Extracting $archivePath ..."
    tar -xf $archivePath -C $HpsDir
}

if (-not (Test-Path $pipelineConfig)) {
    throw "SDK extraction failed; missing $pipelineConfig"
}

$configText = Get-Content -Path $pipelineConfig -Raw -Encoding UTF8
if ($configText -match '(?m)^\s*backend:\s*native\s*$') {
    $serverUrl = ($vlmUrl.TrimEnd("/") + "/v1")
    $configText = [regex]::Replace(
        $configText,
        '(?m)^(\s*)backend:\s*native\s*$',
        "`${1}backend: vllm-server`n`${1}server_url: $serverUrl",
        1
    )
    Set-Content -Path $pipelineConfig -Value $configText -Encoding UTF8 -NoNewline
    Write-Host "Patched pipeline_config.yaml backend native -> vllm-server"
}

$vlmMatch = [regex]::Match(
    $configText,
    '(?ms)module_name:\s*vl_recognition.*?model_name:\s*(\S+)'
)
if (-not $vlmMatch.Success) {
    throw "Failed to read VLM model_name from $pipelineConfig"
}
$vlmName = $vlmMatch.Groups[1].Value
Write-Host " VLM name: $vlmName"

Set-DotEnvValue -Path $envFile -Key "HPS_PIPELINE_NAME" -Value $pipelineName
Set-DotEnvValue -Path $envFile -Key "HPS_SDK_DIR" -Value $sdkDirName
Set-DotEnvValue -Path $envFile -Key "HPS_VLM_URL" -Value $vlmUrl

$entrypointFix = Join-Path $PSScriptRoot "fix-paddleocr-hps-entrypoint.ps1"
if (Test-Path $entrypointFix) {
    & $entrypointFix -HpsDir $HpsDir
}

Write-Host "High-stability serving SDK prepared at $sdkRoot"
Write-Host "Next:"
Write-Host "  .\scripts\patch-paddleocr-hps-port.ps1 -HpsDir `"$HpsDir`" -HostPort 8888"
Write-Host "  cd `"$HpsDir`"; docker compose up -d"
