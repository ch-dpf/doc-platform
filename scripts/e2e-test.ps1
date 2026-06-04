# End-to-end smoke test for doc-platform (requires infra + both services running)
$ErrorActionPreference = "Stop"
$baseIngest = "http://localhost:8081"
$baseVector = "http://localhost:8082"
$tenant = "demo"
$sample = Join-Path $PSScriptRoot "..\samples\knowledge.txt"

if (-not (Test-Path $sample)) {
    throw "Sample file not found: $sample"
}

Write-Host "1. Upload document..."
$uploadJson = curl.exe -s -X POST "$baseIngest/api/v1/documents/upload?tenantId=$tenant&autoIndex=true" `
    -F "file=@$sample"
$upload = $uploadJson | ConvertFrom-Json
$docId = $upload.docId
Write-Host "   docId=$docId status=$($upload.parseStatus)"

Write-Host "2. Wait for parse + index..."
$deadline = (Get-Date).AddSeconds(90)
do {
    Start-Sleep -Seconds 3
    $docJson = curl.exe -s "$baseIngest/api/v1/documents/$docId"
    $doc = $docJson | ConvertFrom-Json
    Write-Host "   parse=$($doc.parseStatus) index=$($doc.indexStatus)"
    if ($doc.parseStatus -eq "PARSED" -and $doc.indexStatus -eq "INDEXED") { break }
    if ((Get-Date) -gt $deadline) { throw "Timeout waiting for indexing" }
} while ($true)

Write-Host "3. Semantic search..."
$body = '{"tenantId":"demo","query":"pgvector semantic search","topK":3}'
$searchJson = curl.exe -s -X POST "$baseVector/api/v1/search" -H "Content-Type: application/json" -d $body
$search = $searchJson | ConvertFrom-Json
if ($search.hits.Count -lt 1) { throw "No search hits returned" }
Write-Host "   top hit score=$($search.hits[0].score)"

Write-Host "3b. RAG chat (requires ollama pull llama3.2)..."
$ragBody = '{"tenantId":"' + $tenant + '","question":"What is pgvector used for in this platform?","topK":3}'
try {
    $ragJson = curl.exe -s -X POST "$baseVector/api/v1/rag/chat" -H "Content-Type: application/json" -d $ragBody
    $rag = $ragJson | ConvertFrom-Json
    if (-not $rag.answer) { throw "Empty RAG answer" }
    Write-Host "   RAG usedLlm=$($rag.usedLlm) citations=$($rag.citations.Count)"
} catch {
    Write-Host "   RAG skipped (install chat model): $_"
}

Write-Host "4. Delete document..."
curl.exe -s -X DELETE "$baseIngest/api/v1/documents/$docId" | Out-Null
Start-Sleep -Seconds 2

$searchAfterJson = curl.exe -s -X POST "$baseVector/api/v1/search" -H "Content-Type: application/json" -d $body
$searchAfter = $searchAfterJson | ConvertFrom-Json
$filtered = $searchAfter.hits | Where-Object { $_.docId -eq $docId }
if ($filtered.Count -gt 0) { throw "Deleted doc still appears in search" }

Write-Host "E2E test passed."
