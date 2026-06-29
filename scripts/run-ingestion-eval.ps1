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
        "IngestionEvalBaselineTest",
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
    $previousErrorAction = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & mvn -q "-Dmaven.repo.local=$MavenRepo" -pl knowbase-ingestion,knowbase-retrieval -am test `
        "-Dtest=$parseArg" `
        "-Dsurefire.failIfNoSpecifiedTests=false" 2>&1 | Out-String | Write-Output
    $mvnExitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorAction
    if ($mvnExitCode -ne 0) {
        throw "Ingestion eval failed with exit code $mvnExitCode"
    }

    $sampleManifest = Get-Content -Raw -Encoding UTF8 "sample-documents/retrieval-eval-samples.json" | ConvertFrom-Json
    $enabledSamples = @($sampleManifest.samples | Where-Object { $_.enabled -ne $false })
    $categoryCounts = @{}
    foreach ($sample in $enabledSamples) {
        if (-not $categoryCounts.ContainsKey($sample.category)) {
            $categoryCounts[$sample.category] = 0
        }
        $categoryCounts[$sample.category] = $categoryCounts[$sample.category] + 1
    }
    $baselinePath = "sample-documents/ingestion-eval-baseline.json"
    $baseline = Get-Content -Raw -Encoding UTF8 $baselinePath | ConvertFrom-Json
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
            baselineVersion = $baseline.version
            minimumAverageCitationScore = $baseline.minimumAverageCitationScore
            note = "Baseline gate enforced by IngestionEvalBaselineTest against ingestion-eval-baseline.json"
        }
        notes = "Offline parse/chunk/citation-metadata regression. Live retrieval eval uses scripts/verify-sample-documents.ps1 against a running backend."
    }
    $reportPath = "sample-documents/ingestion-eval-report.json"
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText((Join-Path $root $reportPath), ($summary | ConvertTo-Json -Depth 8), $utf8NoBom)
    $summary | ConvertTo-Json -Depth 8
} finally {
    Pop-Location
}
