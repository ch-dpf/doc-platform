# Sample Documents Regression Set

Canonical test fixtures live under `knowbase-ingestion/src/test/resources/sample-documents/`.

| Path | Purpose |
|------|---------|
| `markdown/guide.md` | Structure-aware markdown with headings |
| `markdown/faq-outline.md` | Multi-section FAQ outline |
| `plain/long-paragraph.txt` | Token-window split regression |

Run snapshot tests:

```bash
mvn -pl knowbase-ingestion -am test -Dtest=SampleDocumentChunkSnapshotTest
```

This directory mirrors the same samples for documentation discoverability.
