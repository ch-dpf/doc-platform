param(
    [string]$OllamaHost = "http://localhost:11434",
    [string]$VisionModel = "",
    [switch]$SkipVisionPull
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    Write-Host "Creating knowbase-reading-order model from Modelfile..."
    & ollama create knowbase-reading-order -f "infra/ollama/knowbase-reading-order.Modelfile"
    if ($LASTEXITCODE -ne 0) {
        throw "ollama create knowbase-reading-order failed"
    }

    if (-not $SkipVisionPull) {
        if ([string]::IsNullOrWhiteSpace($VisionModel)) {
            $VisionModel = "llama3.2-vision"
        }
        Write-Host "Pulling vision layout model: $VisionModel"
        & ollama pull $VisionModel
        if ($LASTEXITCODE -ne 0) {
            throw "ollama pull $VisionModel failed"
        }
    }

    Write-Host ""
    Write-Host "Enable in application.yml:"
    Write-Host "  knowbase.ingestion.layout.ollama.enabled: true"
    Write-Host "  knowbase.ingestion.layout.default-provider: ollama-layout"
    Write-Host "  knowbase.ingestion.reading-order.provider: ollama   # or http when endpoint is set; falls back to heuristic-bbox"
    Write-Host "  knowbase.ingestion.reading-order.endpoint: http://localhost:8090/reading-order   # optional HTTP-first"
    Write-Host "  knowbase.ollama.vision-language-model: $VisionModel"
} finally {
    Pop-Location
}
