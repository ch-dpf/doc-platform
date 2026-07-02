param(
    [string]$HpsBaseUrl = "http://localhost:8888",
    [string]$ImagePath = "",
    [int]$ReadyTimeoutSec = 300
)

$ErrorActionPreference = "Stop"

function Invoke-HpsGet {
    param(
        [string]$Path,
        [int]$TimeoutSec = 30
    )
    return Invoke-RestMethod -Method Get -Uri "$HpsBaseUrl$Path" -TimeoutSec $TimeoutSec
}

function Wait-HpsReady {
    param([int]$TimeoutSec)

    try {
        $ready = Invoke-HpsGet -Path "/health/ready" -TimeoutSec 10
        if ($ready -is [string]) {
            Write-Host "HPS ready: $ready"
        } else {
            Write-Host "HPS ready response received."
        }
        return
    } catch {
        Write-Host "No /health/ready endpoint (sm120 compose uses /health only); continuing."
    }
}

Write-Host "=== PaddleOCR-VL HPS verification ==="
Write-Host "Base URL: $HpsBaseUrl"

function Show-HpsDiagnostics {
    param([string]$Reason)
    Write-Host ""
    Write-Host "--- Diagnostics ---"
    $uri = [Uri]$HpsBaseUrl
    $hostPort = if ($uri.IsDefaultPort) { $uri.Port } else { $uri.Port }
    $listeners = netstat -ano | Select-String ":$hostPort\s"
    if ($listeners) {
        Write-Host "Port $hostPort is in use:"
        $listeners | ForEach-Object { Write-Host "  $_" }
    } else {
        Write-Host "Port $hostPort has NO listener (HPS gateway not started or wrong port)."
    }
    $hpsContainers = docker ps -a --format "{{.Names}}`t{{.Status}}`t{{.Ports}}" 2>$null |
        Select-String "paddleocr-vl"
    if ($hpsContainers) {
        Write-Host "paddleocr-vl containers:"
        $hpsContainers | ForEach-Object { Write-Host "  $_" }
    } else {
        Write-Host "No paddleocr-vl-* containers found. Run:"
        Write-Host "  cd D:\workspace\PaddleOCR\deploy\paddleocr_vl_docker\hps"
        Write-Host "  docker compose up -d --build"
    }
    $composePs = docker compose -f D:\workspace\PaddleOCR\deploy\paddleocr_vl_docker\hps\compose.yaml ps 2>$null
    if ($composePs) {
        Write-Host "docker compose ps:"
        Write-Host $composePs
    }
    Write-Host "---"
    Write-Host "Common causes:"
    Write-Host "  1) Skipped 'docker compose up -d' after patch port"
    Write-Host "  2) docker compose build failed (Docker Hub timeout -> configure registry mirror)"
    Write-Host "  3) GPU / image pull still in progress (first start can take 30+ min)"
    throw $Reason
}

try {
    $health = Invoke-HpsGet -Path "/health"
    if ($health.errorCode -eq 0 -or ($health -is [string] -and $health -match "ok|healthy|Healthy")) {
        Write-Host "[OK] /health => $($health | ConvertTo-Json -Compress)"
    } elseif ($health -is [string]) {
        Write-Host "[OK] /health => $health"
    } else {
        throw "unexpected health response: $($health | ConvertTo-Json -Compress)"
    }
} catch {
    Show-HpsDiagnostics -Reason "HPS /health failed: $_"
}

Wait-HpsReady -TimeoutSec $ReadyTimeoutSec
Write-Host "[OK] service ready"

if ($ImagePath -and (Test-Path $ImagePath)) {
    Write-Host "Testing /layout-parsing with $ImagePath ..."
    $bytes = [IO.File]::ReadAllBytes($ImagePath)
    $body = @{
        file = [Convert]::ToBase64String($bytes)
        fileType = 1
        prettifyMarkdown = $true
        returnMarkdownImages = $false
        restructurePages = $false
    } | ConvertTo-Json -Depth 5

    $parseResponse = Invoke-RestMethod `
        -Method Post `
        -Uri "$HpsBaseUrl/layout-parsing" `
        -Body $body `
        -ContentType "application/json" `
        -TimeoutSec 600

    $errorCode = $parseResponse.errorCode
    if ($null -ne $errorCode -and [int]$errorCode -ne 0) {
        throw "layout-parsing errorCode=$errorCode msg=$($parseResponse.errorMsg)"
    }

    $results = $parseResponse.result.layoutParsingResults
    if (-not $results -or $results.Count -eq 0) {
        throw "layout-parsing returned no layoutParsingResults"
    }

    $markdown = $results[0].markdown.text
    $blockCount = 0
    $list = $results[0].prunedResult.parsing_res_list
    if ($list) {
        $blockCount = @($list).Count
    }
    $preview = if ($markdown) { $markdown.Substring(0, [Math]::Min(120, $markdown.Length)) } else { "(empty markdown)" }
    Write-Host "[OK] layout-parsing blocks=$blockCount markdownPreview=$preview"
} else {
    Write-Host "Skip layout-parsing smoke test (pass -ImagePath to a PNG/JPG page image)."
}

Write-Host ""
Write-Host "KnowBase application.yml should use:"
Write-Host "  knowbase.vision-document.provider: paddleocr-vl"
Write-Host "  knowbase.vision-document.paddleocr-vl.base-url: $HpsBaseUrl"
Write-Host "Verification complete."
