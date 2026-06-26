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
        "SampleHtmlMergedCellsParseRegressionTest",
        "SamplePdfParseRegressionTest",
        "SampleXlsxParseRegressionTest",
        "SampleOcrHocrRegressionTest",
        "SampleOcrTsvRegressionTest",
        "SampleMarkdownParseRegressionTest",
        "SampleConfigParseRegressionTest",
        "SampleMultiHeaderTableParseRegressionTest",
        "SampleDocumentChunkSnapshotTest",
        "SampleDocumentCatalogCoverageTest",
        "IngestionCitationCompletenessEvaluatorTest",
        "FormulaBlockParseEnricherTest",
        "PdfFormulaDetectorTest",
        "PdfTableCellSpanInferrerTest",
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
    $categoryCounts = @{}
    foreach ($sample in $enabledSamples) {
        if (-not $categoryCounts.ContainsKey($sample.category)) {
            $categoryCounts[$sample.category] = 0
        }
        $categoryCounts[$sample.category] = $categoryCounts[$sample.category] + 1
    }
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
            categoryCounts = $categoryCounts
        }
        citationEval = [ordered]@{
            evaluator = "IngestionCitationCompletenessEvaluator"
            note = "See IngestionCitationCompletenessEvaluatorTest for PDF/table citation field scoring"
        }
        notes = "Offline parse/chunk/citation-metadata regression. Live retrieval eval uses scripts/verify-sample-documents.ps1 against a running backend."
    }
    $reportPath = "sample-documents/ingestion-eval-report.json"
    $summary | ConvertTo-Json -Depth 8 | Set-Content -Encoding UTF8 $reportPath
    $summary | ConvertTo-Json -Depth 8
} finally {
    Pop-Location
}
