param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$DocumentRoot = "D:\document",
    [int]$MaxFiles = 12,
    [string[]]$Extensions = @("md", "pdf", "docx", "xlsx"),
    [string]$Question = "Summarize the installation or business materials covered by these sample documents."
)

$ErrorActionPreference = "Stop"

function Invoke-KnowbaseJson {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null
    )

    $params = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        TimeoutSec = 300
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = ($Body | ConvertTo-Json -Depth 30)
    }
    try {
        $response = Invoke-RestMethod @params
    } catch {
        $errorBody = ""
        if ($_.Exception.Response -and $_.Exception.Response.GetResponseStream()) {
            $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            $errorBody = $reader.ReadToEnd()
        }
        throw "HTTP request failed: $Method $Path $($_.Exception.Message) $errorBody"
    }
    if (-not $response.success) {
        throw "API failed: $($response.code) $($response.message)"
    }
    return $response.data
}

function ConvertTo-FileUri {
    param([string]$Path)
    $resolved = (Resolve-Path -LiteralPath $Path).Path
    return "file://" + $resolved.Replace([char]92, [char]47)
}

function Wait-IngestionRun {
    param(
        [string]$RunId,
        [int]$TimeoutSec = 600
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    $terminalStatuses = @("SUCCEEDED", "PARTIAL_FAILED", "FAILED", "CANCELLED")
    do {
        $run = Invoke-KnowbaseJson -Method Get -Path "/api/v1/ingestion-runs/$RunId"
        if ($terminalStatuses -contains $run.status) {
            return $run
        }
        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $deadline)

    throw "Ingestion run did not finish within $TimeoutSec seconds: $RunId"
}

if (-not (Test-Path -LiteralPath $DocumentRoot)) {
    throw "Sample document directory does not exist: $DocumentRoot"
}

$supportedFiles = Get-ChildItem -LiteralPath $DocumentRoot -Recurse -File |
    Where-Object { $Extensions -contains $_.Extension.TrimStart(".").ToLowerInvariant() } |
    Select-Object -First $MaxFiles

if (-not $supportedFiles) {
    throw "No sample files matched extensions: $($Extensions -join ', ')"
}

$suffix = Get-Date -Format "yyyyMMddHHmmss"
$libraryPath = "/api/v1/libraries"
$agentPath = "/api/v1/agents"
$queryRunPath = "/api/v1/query-runs"

$library = Invoke-KnowbaseJson -Method Post -Path $libraryPath -Body @{
    tenantId = "default"
    name = "Sample Documents Library $suffix"
    description = "Verify heterogeneous ingestion with local sample documents"
    libraryTypePresetCode = "technical_docs"
    tags = @("sample", "heterogeneous", "local-file")
}

$tokenizerProfiles = Invoke-KnowbaseJson -Method Get -Path "/api/v1/tokenizer-profiles?provider=ollama"
$chatTokenizerProfile = @($tokenizerProfiles | Where-Object { $_.modelName -eq "llama3.2" } | Select-Object -First 1)[0]

$ingestionPath = "/api/v1/libraries/$($library.libraryId)/ingestion-runs"
$ingestion = Invoke-KnowbaseJson -Method Post -Path $ingestionPath -Body @{
    libraryId = $library.libraryId
    sourceUris = @((ConvertTo-FileUri -Path $DocumentRoot))
    sourceType = "local_directory"
    documentProfileCode = $null
    publishIndexOnSuccess = $true
    options = @{
        recursive = $true
        maxFiles = $MaxFiles
        extensions = $Extensions
    }
}
if (@("SUCCEEDED", "PARTIAL_FAILED", "FAILED", "CANCELLED") -notcontains $ingestion.status) {
    $ingestion = Wait-IngestionRun -RunId $ingestion.runId
}

$agent = Invoke-KnowbaseJson -Method Post -Path $agentPath -Body @{
    tenantId = "default"
    name = "Sample Documents Agent $suffix"
    description = "Agent for local heterogeneous sample document verification"
    scenePresetCode = "internal_knowledge_assistant"
    libraryIds = @($library.libraryId)
    routingPolicy = @{
        mode = "selected_libraries"
    }
    retrievalPolicy = @{
        topKPerLibrary = 8
        maxEvidence = 12
    }
    answerPolicy = @{
        citationRequired = $true
        refuseWhenEvidenceLow = $true
        maxContextTokens = 4096
    }
    chatTokenizerProfileId = if ($chatTokenizerProfile) { $chatTokenizerProfile.tokenizerProfileId } else { $null }
    systemPrompt = "Answer only from the provided evidence, keep citations, and do not invent unsupported facts."
}

$query = Invoke-KnowbaseJson -Method Post -Path $queryRunPath -Body @{
    agentId = $agent.agentId
    agentVersionId = $null
    sessionId = $null
    question = $Question
    debugLibraryIds = @()
    variables = @{}
    stream = $false
}

$summary = [ordered]@{
    baseUrl = $BaseUrl
    documentRoot = $DocumentRoot
    requestedMaxFiles = $MaxFiles
    requestedExtensions = $Extensions
    discoveredSamples = @($supportedFiles | ForEach-Object { $_.FullName })
    libraryId = $library.libraryId
    ingestionRunId = $ingestion.runId
    ingestionStatus = $ingestion.status
    inputDocuments = $ingestion.inputDocuments
    succeededDocuments = $ingestion.succeededDocuments
    failedDocuments = $ingestion.failedDocuments
    chunkCount = $ingestion.chunkCount
    indexVersionId = $ingestion.indexVersionId
    agentId = $agent.agentId
    agentVersionId = $agent.agentVersionId
    chatTokenizerProfileId = if ($chatTokenizerProfile) { $chatTokenizerProfile.tokenizerProfileId } else { $null }
    queryRunId = $query.queryRunId
    queryStatus = $query.status
    citationCount = @($query.citations).Count
    evidenceCount = @($query.evidence).Count
    answer = $query.answer
}

$summary | ConvertTo-Json -Depth 10
