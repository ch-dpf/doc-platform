param(
    [string]$MavenRepo = ".m2/repository"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $parseTests = @(
        "SampleTableParseRegressionTest",
        "SampleHtmlTableParseRegressionTest",
        "SamplePdfParseRegressionTest",
        "SampleXlsxParseRegressionTest",
        "SampleOcrHocrRegressionTest",
        "SampleOcrTsvRegressionTest",
        "SampleMarkdownParseRegressionTest",
        "SampleConfigParseRegressionTest",
        "SampleMultiHeaderTableParseRegressionTest",
        "SampleDocumentChunkSnapshotTest",
        "LayoutBboxSupportTest",
        "PaddleOcrVlPrunedResultMapperTest",
        "TableGridModelTest",
        "TableGridParseEnricherTest",
        "OcrHocrParserTest",
        "OcrRetrievalDownweightSupportTest",
        "DefaultEvidenceBuilderTest"
    )
    $parseArg = ($parseTests -join ",")
    & mvn -q "-Dmaven.repo.local=$MavenRepo" -pl knowbase-ingestion,knowbase-retrieval -am test `
        "-Dtest=$parseArg" `
        "-Dsurefire.failIfNoSpecifiedTests=false" 2>&1 | Out-String | Write-Output
    if ($LASTEXITCODE -ne 0) {
        throw "Ingestion eval failed with exit code $LASTEXITCODE"
    }

    $sampleManifest = Get-Content -Raw "sample-documents/retrieval-eval-samples.json" | ConvertFrom-Json
    $enabledSamples = @($sampleManifest.samples | Where-Object { $_.enabled -ne $false })
    $summary = [ordered]@{
        status = "passed"
        ranAt = (Get-Date).ToString("o")
        suites = @(
            [ordered]@{
                name = "parse-regression"
                modules = @("knowbase-ingestion", "knowbase-retrieval")
                tests = $parseTests
            }
        )
        retrievalEvalSamples = [ordered]@{
            version = $sampleManifest.version
            enabledCount = $enabledSamples.Count
            categories = @(
                "markdown",
                "plain",
                "table",
                "html",
                "ocr",
                "config",
                "pdf-programmatic",
                "xlsx-programmatic"
            )
        }
        notes = "Offline parse/chunk regression only. Live retrieval eval uses scripts/verify-sample-documents.ps1 against a running backend."
    }
    $summary | ConvertTo-Json -Depth 6
} finally {
    Pop-Location
}
