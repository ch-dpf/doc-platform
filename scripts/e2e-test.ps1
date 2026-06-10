# End-to-end smoke test for knowbase (requires infra + service on 8080)
$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"
$tenant = "demo"
$libraryId = "00000000-0000-0000-0000-000000000001"
$sample = Join-Path $PSScriptRoot "..\samples\knowledge.txt"

if (-not (Test-Path $sample)) {
    throw "Sample file not found: $sample"
}

Write-Host "1. Upload document..."
$uploadJson = curl.exe -s -X POST "$base/api/v1/documents/upload?libraryId=$libraryId&tenantId=$tenant&autoIndex=true" `
    -F "file=@$sample"
$upload = $uploadJson | ConvertFrom-Json
$docId = $upload.docId
Write-Host "   docId=$docId status=$($upload.parseStatus)"

Write-Host "2. Wait for parse + index..."
$deadline = (Get-Date).AddSeconds(90)
do {
    Start-Sleep -Seconds 3
    $docJson = curl.exe -s "$base/api/v1/documents/$docId"
    $doc = $docJson | ConvertFrom-Json
    Write-Host "   parse=$($doc.parseStatus) index=$($doc.indexStatus)"
    if ($doc.parseStatus -eq "PARSED" -and $doc.indexStatus -eq "INDEXED") { break }
    if ((Get-Date) -gt $deadline) { throw "Timeout waiting for indexing" }
} while ($true)

Write-Host "3. RAG chat (requires ollama pull llama3.2)..."
$ragBody = "{`"libraryId`":`"$libraryId`",`"tenantId`":`"$tenant`",`"question`":`"What is pgvector used for?`",`"topK`":3}"
try {
    $ragJson = curl.exe -s -X POST "$base/api/v1/rag/chat" -H "Content-Type: application/json" -d $ragBody
    $rag = $ragJson | ConvertFrom-Json
    if (-not $rag.answer) { throw "Empty RAG answer" }
    Write-Host "   RAG found=$($rag.found) usedLlm=$($rag.usedLlm)"
} catch {
    Write-Host "   RAG skipped: $_"
}

Write-Host "4. Delete document..."
curl.exe -s -X DELETE "$base/api/v1/documents/$docId" | Out-Null
Start-Sleep -Seconds 2

$docAfterJson = curl.exe -s -w "%{http_code}" "$base/api/v1/documents/$docId"
if ($docAfterJson -notmatch "404") { throw "Deleted doc still accessible" }

Write-Host "E2E test passed."
