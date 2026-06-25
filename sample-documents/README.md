# Sample Documents Regression Set

Canonical test fixtures live under `knowbase-ingestion/src/test/resources/sample-documents/`.

| Path | Purpose |
|------|---------|
| `markdown/guide.md` | Structure-aware markdown with headings |
| `markdown/faq-outline.md` | Multi-section FAQ outline |
| `plain/long-paragraph.txt` | Token-window split regression |
| `table/metrics.csv` | Adaptive table CSV with header/data rows and parseConfidence |
| `table/multi-header-metrics.csv` | Two-row header CSV → multi-level `headerPath` on DATA rows |
| `html/team-metrics.html` | HTML table → table_row blocks with tableRegionLabel |
| `html/merged-cells.html` | HTML colspan/rowspan regression |
| `ocr/sample-scan.hocr` | hOCR regression with bbox/confidence and low-confidence flags |
| `ocr/sample-scan.tsv` | Tesseract TSV bbox/confidence regression |
| `config/application-sample.yml` | YAML structure parser regression |
| `external/mock-docling-response.json` | External parser response fixture (optional integration) |

Programmatic regression (no binary fixtures committed):

- `SamplePdfParseRegressionTest` — PDFBox table PDF → layout blocks + `tableGrid` + `evidenceAssetHint`
- `SampleXlsxParseRegressionTest` — POI workbook → adaptive table rows + sheet metadata

Run offline ingestion eval (parse + chunk snapshots):

```powershell
.\scripts\run-ingestion-eval.ps1
```

Run parse regression suite:

```powershell
.\scripts\run-parse-regression.ps1
```

Run snapshot tests only:

```bash
mvn -pl knowbase-ingestion -am test -Dtest=SampleDocumentChunkSnapshotTest
```

Live backend retrieval smoke (requires running app + PostgreSQL):

```powershell
.\scripts\verify-sample-documents.ps1 -DocumentRoot D:\document
```

Retrieval eval question fixtures: `sample-documents/retrieval-eval-samples.json`.

This directory mirrors the same samples for documentation discoverability.
