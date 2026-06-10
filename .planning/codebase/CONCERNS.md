---
last_mapped_commit: 0bb941f00c6dcafdfe063ed48192837a10bb1f57
---

# Codebase Concerns

**Analysis Date:** 2026-06-10

## Tech Debt

**Weekly-report QA bolted onto RAG instead of structured retrieval:**
- Issue: Tabular weekly-report questions (employee rosters, work-item summaries, week-by-project lookups) are handled by a growing stack of regex/rule classes and prompt hints layered on top of vector search, not by querying structured rows.
- Files: `knowbase-service/src/main/java/com/knowbase/vector/rag/RagPromptBuilder.java`, `RagWeeklyReportSummarySupport.java`, `RagWeeklyReportWeekSupport.java`, `RagEmployeeRosterSupport.java`, `WeeklyReportWorkItemExtractor.java`, `RagQuestionAnalyzer.java`, `knowbase-service/src/main/java/com/knowbase/vector/service/RagService.java`
- Why: Excel is flattened to tab-separated plain text via Tika (`DocumentParseService.java`) and chunked as prose (`chunking.strategy: paragraph-first` in `application.yml`); no spreadsheet schema exists in Postgres (`infra/postgres/init.sql` only has `document_chunk.content TEXT`).
- Impact: Fragile regex on `\t`-delimited rows (`WeeklyReportWorkItemExtractor.WORK_ROW`); table rows split across chunks; LLM still invoked for many table questions with 100+ lines of domain prompt rules; each new weekly-report question type adds another `*Support` class.
- Fix approach: Introduce structured ingest for Excel (row/column model or JSONB per sheet), expose aggregation queries for roster/count/week lookups, reserve RAG for free-text synthesis only.

**Single-track vector-only knowledge model:**
- Issue: All indexed knowledge lives as flat text chunks + 768-d embeddings in `document_chunk`; metadata is opaque JSONB with no typed fields for employees, projects, weeks, or deadlines.
- Files: `infra/postgres/init.sql` (`document_chunk`), `knowbase-service/src/main/java/com/knowbase/vector/retrieval/ChunkMetadataBuilder.java`, `knowbase-service/src/main/java/com/knowbase/vector/service/ChunkingService.java`
- Why: MVP path optimized for generic document RAG with pgvector HNSW.
- Impact: Cross-document aggregations (employee counts, project participation, week scans) bypass vector search via library-wide metadata/chunk scans (`RagEmployeeRosterSupport.tryLibraryWideCountAnswer`, `RagWeeklyReportWeekSupport.tryLibraryWideAnswer` iterating `DocMetadataStore.findActiveByLibrary` + `DocumentChunkMapper`); does not scale with library size; hybrid BM25+vector (`RagRetrievalService.java`) cannot rank tabular facts reliably.
- Fix approach: Dual-track model — structured facts table(s) for tabular/entity queries + vector chunks for narrative; or dedicated SQL/OLAP layer for weekly-report dimensions.

**Query-rewrite history includes assistant answers:**
- Issue: `RagQueryRewriteService.rewrite()` passes the last two chat messages (user + assistant) into the rewrite LLM via `trimHistory()`; assistant turns contain long formatted answers, citation markers, and prior-topic keywords.
- Files: `knowbase-service/src/main/java/com/knowbase/vector/rag/RagQueryRewriteService.java` (lines 72–77, 136–141), `knowbase-service/src/main/java/com/knowbase/vector/service/RagRetrievalService.java` (line 139), `knowbase-service/src/main/java/com/knowbase/chat/service/ChatMemoryService.java` (`loadHistory` feeds full assistant content)
- Why: Intended to give rewrite model conversational context; mitigation exists only for metadata-style rewrite output (`looksLikeLibraryMetadataRewrite`).
- Impact: Rewrite query can inherit stale topic terms from prior assistant answers, polluting vector/BM25 retrieval on follow-up turns; `RagQueryRewriteServiceTest.java` has no history-pollution cases.
- Fix approach: Pass user-only turns to rewrite; or strip assistant content to a keyword summary; store `search_query` on `chat_message` (column exists in `init.sql`) and reuse instead of re-deriving from assistant prose.

**Follow-up search-query concatenation:**
- Issue: `RagConversationSupport.resolveSearchQuery()` prepends the previous user question to short follow-ups (`previous + " " + trimmed`), which becomes input to query rewrite and retrieval.
- Files: `knowbase-service/src/main/java/com/knowbase/vector/rag/RagConversationSupport.java` (lines 43–66), tests in `RagConversationSupportTest.java`
- Why: Improve recall for anaphoric short questions ("那负责人呢？").
- Impact: Partially mitigated for employee-count/calendar-year/project questions via `looksLikeFollowUp()` exclusions, but other topic switches can still concatenate unrelated prior queries; combined string up to 80 chars after rewrite can dilute retrieval precision.
- Fix approach: Use explicit coreference resolution or intent classifier; pass structured `{priorIntent, currentQuestion}` instead of string concatenation.

**Excel table extraction limited to plain Tika text:**
- Issue: `TableExtractionMode.STRUCTURED` applies only to the HTML pipeline (`HtmlParsingContentProcessor.java`); Excel files use `extractPlainWithTika()` with no structured table mode.
- Files: `knowbase-service/src/main/java/com/knowbase/ingest/service/DocumentParseService.java`, `knowbase-service/src/main/java/com/knowbase/ingest/parse/TableExtractionMode.java`, `knowbase-service/src/main/java/com/knowbase/library/config/ParsingRulesSettings.java`
- Why: Structured mode built for HTML tables in Word/PDF-derived HTML, not native XLSX.
- Impact: Column boundaries depend on Tika tab output; merged cells, multi-sheet workbooks, and non-standard weekly-report layouts break `WeeklyReportChunkHeuristics` / `WeeklyReportWorkItemExtractor` regex assumptions.
- Fix approach: Dedicated Excel parser (Apache POI) emitting row objects or markdown tables before chunking.

**Header-only chunk filter fallback:**
- Issue: `IndexingChunkFilter.removeHeaderOnlyChunks()` re-indexes all chunks if every chunk is classified header-only.
- Files: `knowbase-service/src/main/java/com/knowbase/vector/chunk/IndexingChunkFilter.java`, `WeeklyReportChunkHeuristics.java`
- Why: Avoid documents with zero vectors when heuristics misfire.
- Impact: Low-value header fragments remain searchable and can surface as top hits for table QA; preview UI exposes `headerOnly` flag (`RagRetrievalService.java`, `QaView.vue`) but indexing still allows them.
- Fix approach: Tighten heuristics; fail ingest with warning; or attach header rows as metadata on data chunks instead of separate chunks.

**Domain-specific prompt/rule sprawl in RagService:**
- Issue: `RagService.java` orchestrates 10+ pre-LLM rule branches (library stats, employee roster, project participation, weekly summary, weekly week, deadline guard, answer echo recovery) before falling back to generic RAG.
- Files: `knowbase-service/src/main/java/com/knowbase/vector/service/RagService.java`, `RagAnswerGuard.java`
- Why: Compensate for vector retrieval failures on structured questions.
- Impact: High cyclomatic complexity (~535 lines); stream and non-stream paths duplicate logic; new question types require touching central service.
- Fix approach: Pluggable `RagAnswerStrategy` registry keyed by `RagQuestionAnalyzer` intent; move weekly-report logic behind a dedicated query router.

## Known Bugs

**Weekly-report table QA unreliable via pure RAG path:**
- Symptoms: Wrong employee names (Excel serial dates treated as IDs), missed submitters, incomplete work-item lists, or LLM echoing raw `fileName=` / `docId=` reference blocks.
- Trigger: Ask synthesis questions ("哪些员工提交了周报", "主要内容汇总", "哪周含某项目") when rule-based branches miss (`tryRuleBasedAnswer` returns empty) and LLM path runs with tab-split chunks.
- Files: `RagPromptBuilder.java` (lines 90–99), `RagWeeklyReportSummarySupport.java`, `RagAnswerGuard.java`, `WeeklyReportWorkItemExtractor.java`
- Workaround: Rule branches and `recoverWeeklySummaryIfEchoed()` in `RagService.java`; prompt explicitly warns about 5-digit Excel dates.
- Root cause: Unstructured chunk retrieval + LLM synthesis on lossy tab text; not all question variants have rule coverage.
- Blocked by: Lack of structured document model (see Tech Debt).

**Query rewrite emits library-metadata-style queries:**
- Symptoms: Retrieval query becomes a knowledge-base field list (e.g. "项目名称、参与人、部门") instead of search terms; retrieval returns irrelevant chunks.
- Trigger: LLM rewrite after prior assistant answer describing library contents.
- Files: `RagQueryRewriteService.java` (`looksLikeLibraryMetadataRewrite`, `fallbackAfterRejectedRewrite`)
- Workaround: Rejection + fallback to `RagSearchQueryEnhancer.expandSynthesisQuery()`; disable rewrite via `retrieval.query-rewrite-enabled: false` in `application.yml`.
- Root cause: Rewrite LLM conflates conversational context with retrieval keywords.

**OCR enabled but tessdata missing on fresh clone:**
- Symptoms: Upload/parse fails with "OCR 已开启但引擎不可用" or silent empty text for scanned PDFs.
- Trigger: `ingest.ocr.enabled: true` in `application.yml` without running `scripts/setup-tesseract.ps1`; `.traineddata` gitignored per `infra/tesseract/README.md`.
- Files: `DocumentOcrService.java`, `DocumentParseService.java`, `infra/tesseract/README.md`
- Workaround: Run setup script; set `enabled: false` until tessdata present.
- Root cause: OCR depends on external language packs and Windows VC++ redistributable; not validated at startup beyond `initSuccess` flag.

## Security Considerations

**No authentication or authorization layer:**
- Risk: Any client knowing the API can read/write all libraries, upload documents, and invoke RAG/chat for any `tenantId`.
- Files: No `spring-boot-starter-security` in `knowbase-service/pom.xml`; controllers under `knowbase-service/src/main/java/com/knowbase/ingest/controller/`, `knowbase-service/src/main/java/com/knowbase/vector/controller/`, `knowbase-service/src/main/java/com/knowbase/chat/controller/`
- Current mitigation: `tenantId` query param compared to library record in `RagService.assertLibraryAccess()` only for RAG; trivially spoofed.
- Recommendations: Add Spring Security (JWT/OAuth2); derive tenant from authenticated principal; reject mismatched tenant at API gateway or filter.

**Client-controlled tenant identity:**
- Risk: Frontend stores `tenantId: 'demo'` in `localStorage` (`frontend/knowbase-ui/src/composables/useLibraryContext.js`); users can switch tenant in browser devtools.
- Current mitigation: None beyond optional library-tenant equality check in RAG.
- Recommendations: Remove client-side tenant override; bind tenant to session/token.

**Permissive CORS and public bind:**
- Risk: `WebConfig.java` allows `allowedOriginPatterns("*")` on `/api/**`; `server.address: 0.0.0.0` in `application.yml` exposes service on all interfaces.
- Current mitigation: Intended for LAN dev access.
- Recommendations: Restrict origins in production; bind to localhost or place behind reverse proxy with auth.

**Default credentials in config:**
- Risk: Postgres (`knowbase/knowbase`), MinIO (`minioadmin/minioadmin`) in `application.yml` committed to repo.
- Current mitigation: Local-dev defaults.
- Recommendations: Externalize via env vars; never use defaults in production.

## Performance Bottlenecks

**Sequential Ollama calls per RAG turn:**
- Problem: Single user question may invoke query rewrite + embedding + chat completion (and conversation summary every 20 messages) against local Ollama.
- Files: `RagQueryRewriteService.java`, `OllamaEmbeddingClient.java`, `OllamaChatClient.java`, `ChatMemoryService.maybeSummarize()`
- Measurement: Not instrumented; `ollama.chat-timeout-seconds: 120`, `timeout-seconds: 60` in `application.yml` imply multi-second latency tolerance.
- Cause: All LLM/embedding traffic to single `localhost:11434` instance; no parallel rewrite+embed pipeline.
- Improvement path: Cache rewrite results per question hash; batch embed; optional async preview; metrics via Micrometer.

**Library-wide scans for rule-based answers:**
- Problem: `RagWeeklyReportWeekSupport.tryLibraryWideAnswer()` loads all active docs and scans chunks per document for project mentions.
- Files: `RagWeeklyReportWeekSupport.java`, `RagProjectParticipationSupport.java`, `DocMetadataStore.java`
- Cause: Cannot answer "which week" or "all projects" from Top-K vector hits alone.
- Improvement path: Precomputed indexes (employee → docs, project → weeks) updated at ingest time.

**OCR PDF rendering:**
- Problem: Each PDF page rendered at 200 DPI and OCR'd sequentially; capped at 50 pages.
- Files: `DocumentOcrService.ocrPdf()`, `OcrProperties` (`max-pdf-pages: 50`, `pdf-render-dpi: 200`)
- Cause: CPU-bound Tess4J + PDFBox in ingest thread.
- Improvement path: Async OCR queue; lower DPI for fallback; page-level parallelism with limits.

**Large multipart uploads:**
- Problem: Batch upload up to 520MB request / 50MB per file (`application.yml`).
- Files: `UploadService.java`, `ingest.max-batch-files: 20`
- Cause: Synchronous parse/index pressure after upload.
- Improvement path: Stream to object storage; back-pressure on index job queue (`document_index_job` table).

## Fragile Areas

**Weekly-report regex extractors:**
- Why fragile: Hard-coded Chinese column names ("序号", "工作内容") and tab-row pattern; skip lists and length heuristics tuned to one report format.
- Files: `WeeklyReportChunkHeuristics.java`, `WeeklyReportWorkItemExtractor.java`, `RagEmployeeNameExtractor.java`
- Common failures: Different column order, English headers, CSV-style commas, extra columns break row matching; empty rule-based answer falls through to LLM.
- Safe modification: Add fixture XLSX files to tests before changing regex; extend `RagWeeklyReportSummarySupportTest.java`, `RagEmployeeNameExtractorTest.java`.
- Test coverage: Unit tests on synthetic tab strings; no tests against real multi-sheet Excel binaries.

**RagQueryRewriteService + conversation history:**
- Why fragile: LLM rewrite with 2-message history window; sanitizer regex-based.
- Files: `RagQueryRewriteService.java`, `RagQueryRewriteServiceTest.java`
- Common failures: Metadata-style rewrite; overly long rewrite; topic bleed from assistant messages.
- Safe modification: Add tests with assistant-heavy history fixtures before changing `trimHistory`.
- Test coverage: No history-pollution regression tests.

**Document OCR initialization:**
- Why fragile: Lazy init with `volatile` flags; single shared `Tesseract` instance; language set per call.
- Files: `DocumentOcrService.java`
- Common failures: Missing tessdata, wrong `data-path`, Windows DLL issues; embedded image OCR silently returns "".
- Safe modification: Health indicator for OCR readiness; integration test with minimal PNG fixture.
- Test coverage: No `DocumentOcrService` tests.

**RagService dual code paths (sync + SSE stream):**
- Why fragile: `chat()` and `streamChat()` duplicate pre-LLM rule chain (~lines 150–260 vs 410–510).
- Files: `RagService.java`, `RagController.java`
- Common failures: Stream path missing a rule fix applied only to sync path (or vice versa).
- Safe modification: Extract shared `prepareAnswer()` pipeline; test both entry points.
- Test coverage: No `RagService` integration tests.

## Scaling Limits

**Single-node Ollama embedding/chat:**
- Current capacity: Bounded by one Ollama instance; `embedding.batch-size: 16`.
- Limit: Concurrent users or large batch re-index jobs queue behind LLM calls.
- Symptoms at limit: Timeouts (`ParseException`, chat timeout), SSE stalls in `QaView.vue`.
- Scaling path: Dedicated embedding service; horizontal Ollama replicas; job queue for index rebuilds.

**Postgres pgvector HNSW per library:**
- Current capacity: All chunks in `document_chunk` with HNSW index (`idx_chunk_embedding`); filtered by `library_id` at query time.
- Limit: Very large libraries increase index size and recall tuning difficulty; no shard per tenant.
- Symptoms at limit: Slower hybrid retrieval, higher memory on Postgres node.
- Scaling path: Partition by `library_id`; separate vector DB (Qdrant/Milvus); reduce `max-candidates` carefully.

**Chat history in Postgres:**
- Current capacity: `chat.max-history-messages: 10` loaded; summary at 20 messages (`ChatMemoryService`).
- Limit: Long conversations still grow `chat_message` table; summary failure leaves unbounded context cost for rewrite/LLM.
- Scaling path: Aggressive summarization; archive old messages; cap stored assistant length.

## Dependencies at Risk

**Tess4J / native Tesseract bindings:**
- Risk: Platform-specific native libs; Windows requires VC++ redistributable (`infra/tesseract/README.md`); tessdata not bundled in repo.
- Impact: OCR silently disabled or ingest failures on new dev machines or Linux prod without apt packages.
- Migration plan: Docker image with `tesseract-ocr-chi-sim`; or cloud OCR API fallback.

**Ollama (local LLM runtime):**
- Risk: External process not managed by Spring; model tags (`llama3.2`, `nomic-embed-text`) can differ across environments.
- Impact: Dimension mismatch vs `embedding.dimension: 768`; behavior drift across model versions.
- Migration plan: Pin model manifests; validate dimension at startup in `OllamaEmbeddingClient`.

**Apache Tika for Excel:**
- Risk: Generic text extraction loses table structure; behavior varies by Tika version.
- Impact: Weekly-report QA quality directly tied to Tika tab formatting.
- Migration plan: Apache POI for XLSX; keep Tika for PDF/Word.

## Missing Critical Features

**Structured document / spreadsheet query layer:**
- Problem: No SQL or API to query "all rows where 工作内容 contains X" or "count distinct 责任人".
- Current workaround: Library-wide Java scans + regex + RAG prompts.
- Blocks: Reliable analytics QA, dashboards, and non-LLM reporting on weekly data.
- Implementation complexity: High (schema design, ingest pipeline, query API).

**Authentication and multi-tenant isolation:**
- Problem: Tenant ID is a string parameter, not enforced identity.
- Current workaround: Single demo tenant in UI.
- Blocks: Production deployment, data isolation, audit trails.
- Implementation complexity: Medium (Spring Security + tenant resolver).

**Excel-aware chunking and ingest:**
- Problem: Same paragraph chunker for prose and spreadsheets.
- Current workaround: Header-only filter + weekly heuristics.
- Blocks: Accurate per-row retrieval for large sheets.
- Implementation complexity: Medium (row-aware chunker, sheet metadata).

**OCR observability and graceful degradation:**
- Problem: Fail-hard on OCR when enabled but unavailable; silent skip for embedded images.
- Current workaround: Manual tessdata setup; disable OCR globally.
- Blocks: Mixed text/image PDF workflows without ops intervention.
- Implementation complexity: Low (startup health check, per-library OCR toggle enforcement).

## Test Coverage Gaps

**End-to-end RAG on weekly-report fixtures:**
- What's not tested: Upload XLSX → index → ask roster/summary/week questions through HTTP or `RagService`.
- Files: No `@SpringBootTest` anywhere in `knowbase-service/src/test`; `RagService.java` untested.
- Risk: Rule-branch regressions and stream/sync divergence ship unnoticed.
- Priority: High
- Difficulty to test: Requires Postgres + Ollama testcontainers or heavy mocks.

**Query rewrite with polluted history:**
- What's not tested: Rewrite behavior when history contains long assistant weekly-report summaries.
- Files: `RagQueryRewriteServiceTest.java` (only empty or single-turn cases)
- Risk: Retrieval pollution on multi-turn QA sessions.
- Priority: High
- Difficulty to test: Low (mock `OllamaChatClient`, pass assistant-heavy history)

**Document OCR and parse pipeline:**
- What's not tested: `DocumentOcrService`, full `DocumentParseService` OCR fallback path.
- Files: `DocumentOcrService.java`, `DocumentParseService.java`
- Risk: OCR config breaks on CI/new developer machines.
- Priority: Medium
- Difficulty to test: Medium (needs tessdata fixture or mocked Tesseract)

**Ingest → index → search integration:**
- What's not tested: Full async upload (`UploadService` → `DocumentPipelineService` → vector index).
- Files: `knowbase-service/src/main/java/com/knowbase/ingest/service/DocumentPipelineService.java`, `knowbase-service/src/main/java/com/knowbase/vector/service/IndexingService.java`
- Risk: Index job failures, idempotency bugs, chunk filter edge cases in production.
- Priority: High
- Difficulty to test: High (MinIO/Postgres/Ollama dependencies)

**Frontend QA flows:**
- What's not tested: No Vitest/Playwright in `frontend/knowbase-ui/package.json`.
- Files: `frontend/knowbase-ui/src/views/QaView.vue`
- Risk: Retrieval preview UI breaks silently against API shape changes.
- Priority: Medium
- Difficulty to test: Medium (component tests with mocked API)

---

*Concerns audit: 2026-06-10*
*Update as issues are fixed or new ones discovered*
