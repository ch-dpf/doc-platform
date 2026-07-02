param(
    [string]$HpsDir = "D:\workspace\PaddleOCR\deploy\paddleocr_vl_docker\hps"
)

$ErrorActionPreference = "Stop"

$entrypoint = Join-Path $HpsDir "genai_server_entrypoint.sh"
if (-not (Test-Path $entrypoint)) {
    throw "Not found: $entrypoint"
}

$content = @'
#!/usr/bin/env sh

set -eu

CONFIG="${PIPELINE_CONFIG:-/config/pipeline_config.yaml}"

VLM_NAME=$(
    grep -A5 'module_name: vl_recognition' "$CONFIG" \
        | grep 'model_name:' \
        | head -1 \
        | awk '{print $2}'
)

if [ -z "$VLM_NAME" ]; then
    echo "Failed to read VLM name from ${CONFIG}" >&2
    exit 1
fi

exec paddleocr genai_server \
    --model_name "$VLM_NAME" \
    --host 0.0.0.0 \
    --port 8080 \
    --backend vllm
'@

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($entrypoint, $content.Replace("`r`n", "`n"), $utf8NoBom)
Write-Host "Normalized LF line endings: $entrypoint"
Write-Host "Restart HPS:"
Write-Host "  cd `"$HpsDir`""
Write-Host "  docker compose down"
Write-Host "  docker compose up -d"
