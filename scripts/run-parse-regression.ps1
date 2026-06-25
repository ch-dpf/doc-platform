param(
    [string]$MavenRepo = ".m2/repository"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $tests = @(
        "SampleTableParseRegressionTest",
        "SampleHtmlTableParseRegressionTest",
        "SamplePdfParseRegressionTest",
        "SampleXlsxParseRegressionTest",
        "SampleOcrHocrRegressionTest",
        "SampleOcrTsvRegressionTest",
        "SampleMarkdownParseRegressionTest",
        "SampleConfigParseRegressionTest",
        "PdfTextExtractabilityAnalyzerTest",
        "PdfAlignedColumnDetectorTest",
        "PdfScannedDocumentRouterTest",
        "OcrHocrParserTest",
        "SampleMultiHeaderTableParseRegressionTest",
        "ExternalParserResponseMapperTest",
        "EvidenceArtifactUriBuilderTest",
        "PdfTableCellBboxAssignerTest",
        "LayoutBboxSupportTest",
        "PaddleOcrVlPrunedResultMapperTest",
        "TableGridModelTest",
        "TableGridParseEnricherTest",
        "DefaultEvidenceBuilderTest",
        "OcrRetrievalDownweightSupportTest"
    )
    $testArg = ($tests -join ",")
    & mvn -q "-Dmaven.repo.local=$MavenRepo" -pl knowbase-ingestion -am test "-Dtest=$testArg" "-Dsurefire.failIfNoSpecifiedTests=false" 2>&1 | Out-String | Write-Output
    if ($LASTEXITCODE -ne 0) {
        throw "Ingestion parse regression tests failed with exit code $LASTEXITCODE"
    }
    & mvn -q "-Dmaven.repo.local=$MavenRepo" -pl knowbase-retrieval -am test "-Dtest=DefaultEvidenceBuilderTest,OcrRetrievalDownweightSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" 2>&1 | Out-String | Write-Output
    $summary = [ordered]@{
        status = "passed"
        ranAt = (Get-Date).ToString("o")
        tests = $tests
        modules = @("knowbase-ingestion", "knowbase-retrieval")
    }
    $summary | ConvertTo-Json -Depth 5
} finally {
    Pop-Location
}
