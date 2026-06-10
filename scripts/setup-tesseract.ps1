# Download Tesseract tessdata language packs into infra/tesseract/tessdata
param(
    [switch]$SkipEng,
    [switch]$CheckOnly
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$tessDir = Join-Path $root "infra\tesseract\tessdata"
$baseUrl = "https://github.com/tesseract-ocr/tessdata/raw/main"

$files = @(
    @{ Name = "chi_sim.traineddata"; MinBytes = 40MB }
)
if (-not $SkipEng) {
    $files += @{ Name = "eng.traineddata"; MinBytes = 1MB }
}

function Test-TessFile {
    param([string]$Path, [long]$MinBytes)
    if (-not (Test-Path $Path)) { return $false }
    return (Get-Item $Path).Length -ge $MinBytes
}

New-Item -ItemType Directory -Force -Path $tessDir | Out-Null

Write-Host "Tessdata directory: $tessDir"
Write-Host ""

if ($CheckOnly) {
    $allOk = $true
    foreach ($file in $files) {
        $path = Join-Path $tessDir $file.Name
        if (Test-TessFile -Path $path -MinBytes $file.MinBytes) {
            $sizeMb = [math]::Round((Get-Item $path).Length / 1MB, 1)
            Write-Host "[OK] $($file.Name) (${sizeMb} MB)"
        } else {
            Write-Host "[MISSING] $($file.Name)"
            $allOk = $false
        }
    }
    Write-Host ""
    if ($allOk) {
        Write-Host "Tessdata ready. Set ingest.ocr.enabled=true in application.yml and restart the service."
        exit 0
    }
    Write-Host "Run without -CheckOnly to download missing files."
    exit 1
}

foreach ($file in $files) {
    $path = Join-Path $tessDir $file.Name
    if (Test-TessFile -Path $path -MinBytes $file.MinBytes) {
        Write-Host "Skip $($file.Name) (already present)"
        continue
    }
    $url = "$baseUrl/$($file.Name)"
    Write-Host "Downloading $($file.Name) ..."
    Write-Host "  $url"
    Invoke-WebRequest -Uri $url -OutFile $path -UseBasicParsing
    if (-not (Test-TessFile -Path $path -MinBytes $file.MinBytes)) {
        throw "Download failed or file too small: $path"
    }
    $sizeMb = [math]::Round((Get-Item $path).Length / 1MB, 1)
    Write-Host "  Done (${sizeMb} MB)"
}

Write-Host ""
Write-Host "Tessdata installed."
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. Edit knowbase-service/src/main/resources/application.yml"
Write-Host "       ingest.ocr.enabled: true"
Write-Host "       ingest.ocr.data-path: ./infra/tesseract/tessdata"
Write-Host "  2. Restart knowbase-service (WorkingDirectory = repo root, e.g. scripts/start-services.ps1)"
Write-Host "  3. Enable OCR in library parsing settings when creating/editing a library"
Write-Host ""
Write-Host "Verify: .\scripts\setup-tesseract.ps1 -CheckOnly"
