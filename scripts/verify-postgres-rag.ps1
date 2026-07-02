param(
    [string]$BaseUrl = "http://localhost:8088"
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
        TimeoutSec = 30
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = ($Body | ConvertTo-Json -Depth 20)
    }
    $response = Invoke-RestMethod @params
    if (-not $response.success) {
        throw "API failed: $($response.code) $($response.message)"
    }
    return $response.data
}

function Wait-IngestionRun {
    param(
        [string]$RunId,
        [int]$TimeoutSec = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    $terminalStatuses = @("SUCCEEDED", "PARTIAL_FAILED", "FAILED", "CANCELLED")
    do {
        $run = Invoke-KnowbaseJson -Method Get -Path "/api/v1/ingestion-runs/$RunId"
        if ($terminalStatuses -contains $run.status) {
            return $run
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "Ingestion run did not finish within $TimeoutSec seconds: $RunId"
}

$suffix = Get-Date -Format "yyyyMMddHHmmss"
$library = Invoke-KnowbaseJson -Method Post -Path "/api/v1/libraries" -Body @{
    tenantId = "default"
    name = "Integration Library $suffix"
    description = "PostgreSQL pgvector Flyway verification library"
    libraryTypePresetCode = "product_knowledge"
    tags = @("integration", "postgres", "pgvector")
    profile = @{
        embeddingProvider = "ollama"
        embeddingModel = "bge-m3"
        embeddingDimension = 1024
        embeddingTokenizerProfileId = $null
        chunkMaxTokens = 128
        chunkOverlapTokens = 16
        retrievalTopK = 5
        options = @{}
    }
    documentProfiles = @(
        @{
            contentFamily = "RICH_TEXT"
            parserCode = "text"
            chunkingStrategy = "structure_token_window"
            tokenizerProfileId = $null
            metadataSchema = @{}
            options = @{}
        }
    )
}

$content = @"
KnowBase supports a PostgreSQL pgvector persistence mode.
Flyway creates the vector extension and the KnowBase schema.
Ingestion stores chunks and embeddings in PostgreSQL.
Question answering retrieves evidence from the published index version.
"@

$ingestion = Invoke-KnowbaseJson -Method Post -Path "/api/v1/libraries/$($library.libraryId)/ingestion-runs" -Body @{
    libraryId = $library.libraryId
    sourceUris = @("verify://postgres-rag-$suffix.md")
    sourceType = "inline"
    documentProfileCode = "default_markdown"
    options = @{
        content = $content
    }
}
if (@("SUCCEEDED", "PARTIAL_FAILED", "FAILED", "CANCELLED") -notcontains $ingestion.status) {
    $ingestion = Wait-IngestionRun -RunId $ingestion.runId
}

$tokenizerProfiles = Invoke-KnowbaseJson -Method Get -Path "/api/v1/tokenizer-profiles?provider=ollama"
$chatTokenizerProfile = @($tokenizerProfiles | Where-Object { $_.modelName -eq "llama3.2" } | Select-Object -First 1)[0]

$agent = Invoke-KnowbaseJson -Method Post -Path "/api/v1/agents" -Body @{
    tenantId = "default"
    name = "Integration Agent $suffix"
    description = "Agent for PostgreSQL pgvector verification"
    scenePresetCode = "internal_knowledge_assistant"
    libraryIds = @($library.libraryId)
    routingPolicy = @{
        mode = "selected_libraries"
    }
    retrievalPolicy = @{
        topKPerLibrary = 5
    }
    answerPolicy = @{
        citationRequired = $true
        refuseWhenEvidenceLow = $true
    }
    chatTokenizerProfileId = if ($chatTokenizerProfile) { $chatTokenizerProfile.tokenizerProfileId } else { $null }
    systemPrompt = "Answer with evidence from the provided context."
}

$query = Invoke-KnowbaseJson -Method Post -Path "/api/v1/query-runs" -Body @{
    agentId = $agent.agentId
    agentVersionId = $null
    sessionId = $null
    question = "What does KnowBase use PostgreSQL pgvector for?"
    debugLibraryIds = @()
    variables = @{}
    stream = $false
}

$summary = [ordered]@{
    baseUrl = $BaseUrl
    libraryId = $library.libraryId
    ingestionRunId = $ingestion.runId
    ingestionStatus = $ingestion.status
    indexVersionId = $ingestion.indexVersionId
    chunkCount = $ingestion.chunkCount
    agentId = $agent.agentId
    agentVersionId = $agent.agentVersionId
    chatTokenizerProfileId = if ($chatTokenizerProfile) { $chatTokenizerProfile.tokenizerProfileId } else { $null }
    queryRunId = $query.queryRunId
    queryStatus = $query.status
    citationCount = @($query.citations).Count
    evidenceCount = @($query.evidence).Count
    answer = $query.answer
}

$summary | ConvertTo-Json -Depth 8
