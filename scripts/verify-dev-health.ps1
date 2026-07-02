param(
    [int]$ApiPort = 8088,
    [int]$UiPort = 5173,
    [int]$PostgresPort = 5433
)

$ErrorActionPreference = "Continue"
$ok = $true

function Test-TcpPort {
    param([int]$Port)
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $client.Connect("127.0.0.1", $Port)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

Write-Host "KnowBase dev health check" -ForegroundColor Cyan

if (Test-TcpPort -Port $PostgresPort) {
    Write-Host "[OK] PostgreSQL port $PostgresPort" -ForegroundColor Green
} else {
    Write-Host "[FAIL] PostgreSQL port $PostgresPort — run .\scripts\start-infra.ps1" -ForegroundColor Red
    $ok = $false
}

if (Test-TcpPort -Port $ApiPort) {
    Write-Host "[OK] Backend port $ApiPort" -ForegroundColor Green
    try {
        $r = Invoke-WebRequest -Uri "http://127.0.0.1:$ApiPort/actuator/health" -UseBasicParsing -TimeoutSec 3 -ErrorAction SilentlyContinue
    } catch {
        try {
            $r = Invoke-WebRequest -Uri "http://127.0.0.1:$ApiPort/v3/api-docs" -UseBasicParsing -TimeoutSec 3
            if ($r.StatusCode -eq 200) {
                Write-Host "      API docs reachable" -ForegroundColor DarkGreen
            }
        } catch {
            Write-Host "      Port open but API not responding yet" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "[--] Backend port $ApiPort not listening" -ForegroundColor Yellow
}

if (Test-TcpPort -Port $UiPort) {
    Write-Host "[OK] Frontend port $UiPort" -ForegroundColor Green
} else {
    Write-Host "[--] Frontend port $UiPort not listening" -ForegroundColor Yellow
}

if (Test-TcpPort -Port 11434) {
    Write-Host "[OK] Ollama port 11434 (optional)" -ForegroundColor Green
} else {
    Write-Host "[--] Ollama not running (optional for dev profile)" -ForegroundColor DarkGray
}

if (-not $ok) { exit 1 }
