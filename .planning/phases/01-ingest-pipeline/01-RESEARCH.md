# Phase 1: 全链路流程梳理 - Research

**Researched:** 2026-06-10
**Domain:** Brownfield ingest pipeline documentation (Spring Boot + Vue SPA, no code changes)
**Confidence:** HIGH

## Summary

Phase 1 delivers `.planning/docs/INGEST-PIPELINE.md` as a **target-state-first** reference describing library creation and single-document ingest from UI/API through `document_chunk` persistence. The brownfield codebase already centralizes per-library rules in `vector_library.config_json` resolved by `LibraryConfigResolver`; the async path is upload → parse/normalize/clean → optional index event → chunk/embed/store.

Research traced all PIPE-01/02/03 anchor classes end-to-end. The planner should structure the deliverable as a **dual-audience document**: an operations-facing decision tree + anti-pattern gallery up front, and a developer-facing stage/class/API matrix with mermaid flow diagrams. Every major section ends with a **「当前差距」** subsection referencing four mandatory anchors: `DocumentPipelineService`, `IndexingService`, `IngestView.vue`, `VectorLibraryConfigMerger`.

Critical gaps to document (not fix in Phase 1): (1) `lockPipeline` is a **hard lock** once any document or chunk exists; (2) `IngestView` `overrideChunk` affects **preview only** despite UI copy claiming it applies to ingest; (3) preview uses `ParsePreviewService` + `ChunkPreviewService` while index uses stored `parsed.txt` via `IndexingService`, with normalization/cleaning ordering differences; (4) v1 has **no ingest-level profile**—`documentMetadata` is semantic tags only via `ChunkMetadataBuilder`.

**Primary recommendation:** Write INGEST-PIPELINE.md as target-state narrative with embedded config matrix (rows=rules, columns=system/library-default/ingest-override), one end-to-end mermaid for library creation and one for document ingest, plus appendix A「当前差距」 keyed to the four anchor classes and appendix B「反模式样本」 citing `ChunkPreviewServiceTest` 杜鹏飞 fixture.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### 规划基线（目标态 vs 现状）

- **D-01:** 流程文档以**合理目标架构**为主叙述，不以「仅描述现有代码」为约束；每章末尾或附录用「当前差距」对照 `LibraryConfigResolver`、 `lockPipeline`、`overrideChunk` 等现状。
- **D-02:** 愿景与 v1 交付拆分——愿景包含通用库一等公民；v1 里程碑交付流程文档 + 召回层质量准则 + 反模式样本，**不**将 RAG 答对率纳入 v1 工程验收。

#### 三层配置边界

- **D-03（系统级）:** 仅固定**基础设施上限**——单文件/批量大小、OCR 引擎可用性、全局 embedding 模型兜底；不承载业务型解析/分块策略。
- **D-04（库级）:** **必须统一**的规则仅限**向量 + 检索 + 治理上限**——embedding 模型/维度、hybrid/rerank、`metadataFilterFields` 白名单、容量、版本策略、支持类型上限。
- **D-05（库级默认，可采集覆盖）:** 解析、清洗、分块策略作为**库级默认值**，目标态允许采集级覆盖（OCR、chunk 参数、语义标签等）；与现状「整条管道锁在库级」的差异写入「当前差距」。
- **D-06（采集级）:** v1 **不实现**持久化 ingest profile；文档规划采集级能力（覆盖项白名单、预览=入库、写入 `doc_metadata` 或 ingest job）。`documentMetadata` 在目标态为**语义标签**（检索过滤），**不**驱动解析/分块管道（除非未来扩展 ingest profile）。
- **D-07（文档呈现）:** 使用**配置矩阵表**——行=规则项，列=系统/库默认/采集覆盖，单元格标注「必须 / 默认 / 可覆盖 / 禁止」。

#### 库类型谱系与建库策略

- **D-08:** 定义**两类**一等公民库：**垂直专用库**（同质语义，可混合文件类型）、**通用混合库**（多类型多语义）。
- **D-09:** **通用混合库为一等公民**（目标态）——通过 MIME 自动默认 + 采集语义标签 + 库级检索配置，力争分块质量可支撑 RAG；v1 文档描述路径，实现分阶段落地。
- **D-10:** 垂直专用库判定用**启发式清单**——同语义 + 同问答模式 → 专用库；异质语义即使同扩展名（如周报 xlsx vs 报销 xlsx）**应拆库**。
- **D-11:** **以语义为主轴**——同质语义允许混合类型（例：周报 xlsx + 导出 pdf 同库），靠 MIME 感知默认分化处理；不以「文件扩展名」作为建库唯一依据。
- **D-12:** 建库流程文档包含**选型决策树**——业务场景 → 库类型（专用/通用）→ 预设（Phase 3）→ 高级微调。

#### 入库流程与配置锁定

- **D-13:** 流程粒度 = **阶段 + 关键类**（对齐 PIPE-03），覆盖：上传 → 解析 → 清洗 → 分块 → `IndexingChunkFilter` → 嵌入 → `document_chunk`。
- **D-14:** 预览与入库一致性——文档**标注现状缺口**（`overrideChunk` 仅预览、索引走库级；`ChunkPreviewService` vs `IndexingService` 路径差异），目标态与 Phase 4（PARITY）对齐。
- **D-15:** `lockPipeline` 目标态 = **软锁定**——允许修改 parsing/cleaning/chunking/embedding 规则，但必须触发**全库重索引**任务，UI 强警告；现状硬锁定记入「当前差距」。

#### 分块质量「高可用」

- **D-16:** 质量**北极星** = **RAG 可答率**（块内信息足以支撑正确回答）；v1 文档验收层 = **检索可召回**（块自洽、非纯表头、关键事实不无故跨块断裂、预览=入库目标态）。
- **D-17:** Phase 1 写**通用质量准则**；按类型的细表引用 Phase 2（`FILE-TYPE-PROCESSING.md`），不在 Phase 1 重复展开。
- **D-18:** 必须包含**反模式对照 + 真实样本**（杜鹏飞周报 xlsx、扫描 PDF OCR 关闭、表头块占比过高等）。
- **D-19:** v2 质量门禁（GATE-01/02）在文档中**列入 backlog 引用**，定义准则但不承诺 v1 实现。

### Claude's Discretion

- 决策树与配置矩阵的具体章节编号、附录命名（`INGEST-PIPELINE.md` 内嵌 vs 拆 `CONFIG-TIERS.md`）由 planner 根据可读性决定，须保证运营与开发各有一份可追踪入口。
- 「当前差距」附录的粒度：至少覆盖 `DocumentPipelineService`、`IndexingService`、`IngestView.vue`、`VectorLibraryConfigMerger` 四类锚点。

### Deferred Ideas (OUT OF SCOPE)

- **结构化双轨 + QueryRouter** — `document_record`、表格枚举查询分流；PROJECT Out of Scope，另立里程碑。
- **v1 采集级 ingest profile 实现** — OCR/chunk 覆盖持久化、预览=入库工程落地 → Phase 4 + backlog。
- **MIME 自动默认引擎实现** — 目标态文档描述，实现可放 Phase 2/3 与预设联动。
- **软锁定 + 全库重索引任务** — 目标态文档描述，工程实现 backlog（替代现状 `VectorLibraryConfigMerger` 硬锁）。
- **RAG 答对率自动化验收** — 北极星指标，非本里程碑测试范围；可手工案例附录。
- **按文件类型质量细表** — Phase 2 `FILE-TYPE-PROCESSING.md`。
- **库预设 UI** — Phase 3 `libraryPresets.js` + 向导套用。
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PIPE-01 | 文档描述建库流程（向导字段 → `config_json` → `LibraryConfigResolver` 生效路径） | Traced `CreateLibraryWizard.vue` → `libraryDefaults.js` → `POST /api/v1/vector-libraries` → `VectorLibraryService.create` → `config_json`; resolver methods mapped per field |
| PIPE-02 | 文档描述单文档入库全流程（上传 → 解析 → 清洗 → 分块 → `IndexingChunkFilter` → 嵌入 → `document_chunk`） | Traced `IngestView` → `DocumentController` → `UploadService` → `DocumentIngestor` → `DocumentPipelineService` → `DocumentIndexCoordinator` → `IndexingService` → `DocumentChunkMapper` |
| PIPE-03 | 文档标注各阶段关键类与 API（含前端入口与后端服务对照） | Full stage/API matrix below; frontend `api/*.js` paths verified against controllers |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Library creation wizard & settings UI | Browser / Client | API / Backend | Vue forms collect config; persistence is server-side `vector_library` row |
| `config_json` persistence & merge | API / Backend | Database / Storage | `VectorLibraryService` writes JSONB; `VectorLibraryConfigMerger` enforces lock rules |
| Config resolution per pipeline stage | API / Backend | — | `LibraryConfigResolver.*For(libraryId)` is single runtime entry for all stages |
| File upload & metadata | API / Backend | Object storage | `DocumentIngestor` validates MIME/size, stores raw bytes, upserts `doc_metadata` |
| Parse / normalize / clean | API / Backend (async) | Object storage | `DocumentPipelineService.processAsync` writes `parsed.txt` to storage |
| Chunk / embed / pgvector insert | API / Backend (async) | Database / Storage | `IndexingService.index` reads parsed text, writes `document_chunk` |
| Chunk preview (no persist) | API / Backend | Browser / Client | `ParsePreviewService` + `ChunkPreviewService` via `IngestView`; does not write chunks |
| Semantic tags on ingest | API / Backend | Database / Storage | `documentMetadata` → `doc_metadata.custom_metadata_json` → `ChunkMetadataBuilder` only |
| Hybrid search / RAG retrieval | API / Backend | Database / Storage | Uses `retrieval.*` from library config; out of Phase 1 flow doc except as downstream consumer |
| System limits (file size, OCR engine, global MIME) | API / Backend (config) | — | `application.yml` + `IngestProperties`; not overridable per document in v1 |

## Standard Stack

> Phase 1 is documentation-only — no new packages. Deliverable uses Markdown + Mermaid (rendered by GitHub/Cursor/IDE).

### Core (existing codebase — document these, do not replace)

| Component | Version / Location | Purpose | Why Standard |
|-----------|-------------------|---------|--------------|
| Spring Boot | 3.2.4 (`pom.xml`) | Backend monolith | Existing deployable; all pipeline services live here |
| Vue 3 + Element Plus | `frontend/knowbase-ui` | Ops/dev UI for library + ingest | Existing wizard and IngestView are doc anchors |
| PostgreSQL + pgvector | `infra/postgres/init.sql` | Metadata, chunks, embeddings | Single source of truth for `document_chunk` |
| Apache Tika + Tess4J | `DocumentParseService`, `DocumentOcrService` | Parse/OCR | Current parse path; Excel is Tika tab-text only |
| Ollama | `application.yml` `ollama.*` | Embedding + chat | Library `embeddingModel` falls back to global default |

### Supporting (documentation tooling)

| Tool | Purpose | When to Use |
|------|---------|-------------|
| Mermaid `flowchart` / `sequenceDiagram` | End-to-end visuals in INGEST-PIPELINE.md | PIPE-01/02 primary diagrams |
| Markdown tables | Config matrix, stage/API matrix | D-07 compliance |
| Existing JUnit fixtures | Anti-pattern code citations | 杜鹏飞 sample in `ChunkPreviewServiceTest` |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Single INGEST-PIPELINE.md | Split CONFIG-TIERS.md | Split improves matrix maintenance; user discretion allows either if dual TOC provided |
| Mermaid in repo docs | Draw.io exports | Mermaid diffs with git; preferred for brownfield |

**Installation:** N/A — no packages added in Phase 1.

## Package Legitimacy Audit

> **N/A** — Phase 1 delivers documentation only; no external package installation.

## Architecture Patterns

### System Architecture Diagram (target-state ingest — for INGEST-PIPELINE.md)

```mermaid
flowchart TB
  subgraph Client["Browser — Vue SPA"]
    Wiz["CreateLibraryWizard"]
    Ingest["IngestView"]
    Edit["EditLibrarySettingsDrawer"]
  end

  subgraph API["API — knowbase-service"]
    VLC["VectorLibraryController"]
    DC["DocumentController"]
    IAC["IndexAdminController"]
    LCR["LibraryConfigResolver"]
    DI["DocumentIngestor"]
    DPS["DocumentPipelineService"]
    DIC["DocumentIndexCoordinator"]
    IS["IndexingService"]
    CPS["ChunkPreviewService"]
    PPS["ParsePreviewService"]
  end

  subgraph Storage["Persistence"]
    VL[("vector_library.config_json")]
    DM[("doc_metadata")]
    DT[("document_chunk")]
    OS[("Object storage raw + parsed.txt")]
  end

  Wiz -->|POST /vector-libraries| VLC
  Edit -->|PUT /vector-libraries/{id}| VLC
  VLC --> VL

  Ingest -->|POST /documents/upload| DC
  Ingest -->|POST /documents/parse-preview| DC
  Ingest -->|POST /index/chunk-preview| IAC
  DC --> DI
  DI --> DM
  DI --> OS
  DI --> DPS
  DPS --> LCR
  DPS --> OS
  DPS --> DIC
  DIC --> IS
  IS --> LCR
  IS --> DT

  DC --> PPS
  IAC --> CPS
  PPS --> LCR
  CPS -.->|preview only| Ingest

  VL --> LCR
```

### Recommended INGEST-PIPELINE.md Structure

Dual-audience layout (recommended by research):

```
1. 读者指南（运营 / 开发入口）
2. 库类型选型决策树（D-08–D-12）→ 链 Phase 3 预设占位
3. 建库流程（PIPE-01）
   3.1 向导字段 ↔ config_json 字段对照
   3.2 config_json → LibraryConfigResolver 生效点
   3.3 当前差距（lockPipeline 硬锁）
4. 单文档入库流程（PIPE-02）
   4.1 Mermaid 阶段图
   4.2 阶段 × 关键类 × API 表（PIPE-03）
   4.3 当前差距（预览分叉、overrideChunk、双次清洗）
5. 三层配置矩阵（D-03–D-07）
6. 分块质量准则（D-16–D-17，引用 Phase 2）
7. 反模式对照（D-18）
8. 附录 A — 当前差距详表（四类锚点）
9. 附录 B — backlog（PARITY, GATE, ingest profile, 软锁定）
```

**Ops entry:** Sections 2, 6, 7 + decision tree + anti-patterns table.
**Dev entry:** Sections 3–5 + appendix A + stage/API matrix.

### Pattern 1: Library config single source

**What:** All pipeline stages read rules via `LibraryConfigResolver` methods on `vector_library.config_json`, with global fallbacks from `application.yml`.

**When to use:** Document every config field’s resolver method and consuming service.

**Resolver method map** [VERIFIED: codebase grep + codegraph]:

| Config field / group | Resolver method | Primary consumer |
|---------------------|-----------------|------------------|
| `parsing.*` | `parseOptionsFor(libraryId)` | `DocumentParseService` (via `DocumentPipelineService`, `ParsePreviewService`) |
| `textNormalizationEnabled`, `textNormalization.*` | `normalizationFor(libraryId)` | `DocumentPipelineService`; `ChunkPreviewService` (preview path) |
| `cleaning.*` | `cleaningFor(libraryId)` | `DocumentPipelineService`, `IndexingService`, `ChunkPreviewService` |
| `chunkingStrategy`, `chunkSize`, `chunkOverlap`, `min/maxChunkSize`, `minParagraphLength`, `normalizeBeforeChunk`, `semanticSimilarityThreshold` | `chunkingFor(libraryId)` | `IndexingService` → `ChunkingService` |
| `embeddingProvider/Model/Dimension` | `embeddingFor(libraryId)` | `LibraryEmbeddingService` in `IndexingService` |
| `retrieval.*` | `retrievalFor(libraryId)` | `VectorSearchService`, `RagRetrievalService`; `retainChunkMetadata` → `ChunkMetadataBuilder` |
| `governance.ingestReviewMode` | `requiresManualReview(libraryId)` | `DocumentIngestor.resolveIndexRequested` |
| `ingestAccess.supportedFileTypes` | `allowedMimeTypes(libraryId)` | `UploadService`, `DocumentIngestor`, `MimeTypeAllowlist` |
| `ingestAccess.capacityLimits` | `capacityLimitsFor(libraryId)` | `LibraryCapacityValidator` |
| `ingestAccess.versionPolicy` | `versionPolicyFor(libraryId)` | `DocumentIngestor` duplicate handling |
| `ingestSourceMode` | `isUploadAllowed`, `requireUploadAllowed` | Blocks crawl-only libraries from upload |
| `tags`, `wizardMode`, `configVersion` | Direct on config object | List/filter UI; audit only |

**System-tier fields** (not in `config_json`) [VERIFIED: `application.yml`]:

| Property | Applies at |
|----------|------------|
| `ingest.max-file-size`, `max-batch-files`, `async-upload-threshold` | `UploadService`, `DocumentIngestor.validateFileSize` |
| `ingest.ocr.enabled`, `data-path`, `language` | `DocumentOcrService` availability; library `parsing.ocrEnabled` gates use |
| `ingest.allowed-mime-types` | Global MIME fallback when library list empty |
| `ollama.embedding-model`, `embedding.dimension` | Fallback when library embedding fields blank |
| `chunking.semantic-similarity-threshold` | Fallback when library threshold ≤ 0 |

### Pattern 2: Post-commit async pipeline

**What:** Upload transaction commits before parse/index async work runs.

**When to use:** Document why version races are avoided.

```java
// DocumentPipelineService.scheduleProcessAfterCommit — after DB commit
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override
    public void afterCommit() {
        processAsync(docId, version, fileBytes, fileName);
    }
});
```

### Pattern 3: Ingest-level semantic tags only (v1)

**What:** `documentMetadata` JSON is stored on `doc_metadata.custom_metadata_json` and merged into chunk metadata for retrieval filtering — it does **not** alter parse/chunk parameters.

**Flow:** `IngestView.resolveDocumentMetadataParam()` → upload query param → `DocumentCustomMetadataSupport.normalizeJson` → `ChunkMetadataBuilder.mergeCustomMetadata`.

### Anti-Patterns to Avoid (document these as 反模式)

- **Misleading overrideChunk copy:** UI says「仅本次预览与入库」but `uploadDocument` sends only `documentMetadata`, never chunk overrides [VERIFIED: `IngestView.vue` + `ingest.js`].
- **Assuming structured table mode helps Excel:** `tableExtraction: structured` applies to HTML pipeline only [VERIFIED: CONCERNS.md + `DocumentParseService`].
- **Using semantic chunking for weekly-report Excel:** Paragraph-first + text-only is the documented correct path; semantic splitting breaks tabular continuity [VERIFIED: PROJECT.md + `ChunkPreviewServiceTest`].
- **Treating preview block count as indexed count without same path:** Preview uses `ParsePreviewService` (extract only) + `ChunkPreviewService`; index uses stored `parsed.txt` after pipeline normalize/clean [VERIFIED: service code paths].

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Config field → runtime effect mapping | Ad-hoc grep in doc | `LibraryConfigResolver` method table above | Resolver is already the seam; duplicating logic in prose drifts |
| Pipeline sequence diagram | Custom drawing tool only | Mermaid in markdown | Version-controlled, matches GSD doc conventions |
| Weekly-report anti-pattern sample text | Invented fixture | `ChunkPreviewServiceTest.previewUsesIndexingChunkFilterAndLibraryChunkParams` sample | Real tab-separated 杜鹏飞 rows used in CI |
| Library-type decision criteria | File-extension-only rules | Semantic-axis heuristic (D-10, D-11) | User-locked; matches business reality (周报 vs 报销 xlsx) |

**Key insight:** Phase 1 value is aligning ops and dev on **where rules bind**, not inventing new abstractions. The codebase already has the seams — the doc must name them precisely.

## Common Pitfalls

### Pitfall 1: Documenting as-is only

**What goes wrong:** Doc becomes obsolete the moment Phase 3–5 land; ops cannot plan MIME defaults or soft-lock.

**Why it happens:** Brownfield mapping temptation.

**How to avoid:** Lead each chapter with target-state (D-01); isolate gaps in appendix A.

**Warning signs:** No mention of ingest profile, MIME auto-default, or soft reindex.

### Pitfall 2: Conflating preview path with index path

**What goes wrong:** Operators trust chunk count from IngestView preview; indexed count differs.

**Why it happens:** Two services (`ChunkPreviewService` vs `IndexingService`), parse preview skips pipeline normalize order, `overrideChunk` preview-only.

**How to avoid:** Explicit side-by-side table in §4.3; link Phase 4 PARITY requirements.

**Warning signs:** Doc says preview equals index without caveats.

### Pitfall 3: lockPipeline described as soft-lock

**What goes wrong:** Ops attempt to change OCR/chunk settings on populated library.

**Why it happens:** Target-state D-15 vs current `VectorLibraryConfigMerger` early return when `lockPipelineConfig=true`.

**How to avoid:** Document current hard lock in `VectorLibraryService.updateSettings` (lines 165–166) and UI `EditLibrarySettingsDrawer` / `IngestView` disabled controls.

**Warning signs:** Doc promises reindex-on-change without noting backlog.

### Pitfall 4: 杜鹏飞 sample with wrong expected chunk count

**What goes wrong:** Doc claims「4 chunk 全召回」while unit test with `chunkSize=500` yields **3 indexed chunks** (1 header filtered).

**Why it happens:** User manual verification used different library params than `ChunkPreviewServiceTest`.

**How to avoid:** Cite test numbers (`rawTotalChunks=4`, `filteredOutCount=1`, `totalChunks=3`) and note user-verified 4-chunk outcome depends on chunk params / filter behavior; tie to quality criterion「检索可召回」not raw count alone.

**Warning signs:** Single magic number without parameter context.

## Code Examples

### PIPE-01: Library creation path

```java
// VectorLibraryService.create — persists config_json
VectorLibraryConfig cfg = request.config() != null ? request.config() : defaultConfig();
VectorLibraryConfigFactory.applyPhase2Defaults(cfg, ingestProperties.getAllowedMimeTypes(), cfg.getWizardMode());
lib.setConfigJson(JsonSupport.toJson(cfg));
mapper.insert(lib);
```

Frontend payload shape from `libraryDefaults.js` `buildCreatePayload` → `POST /api/v1/vector-libraries` via `createVectorLibrary`.

### PIPE-02: Ingest index path

```java
// IndexingService.index — chunk → filter → embed → insert
text = documentCleaningService.apply(text, libraryConfigResolver.cleaningFor(libraryId));
ChunkingProperties chunking = libraryConfigResolver.chunkingFor(libraryId);
List<String> chunks = chunkingService.chunk(libraryId, text, chunking);
chunks = IndexingChunkFilter.removeHeaderOnlyChunks(chunks);
List<float[]> embeddings = libraryEmbeddingService.embedBatch(libraryId, chunks);
chunkMapper.insertChunk(/* ... */, chunks.get(i), chunkMetadataJson, embeddings.get(i));
```

### lockPipeline hard merge

```java
// VectorLibraryService.updateSettings
boolean lockPipeline = documentCount > 0 || chunkCount > 0;
VectorLibraryConfigMerger.mergeSafeFields(existing, request.config(), lockPipeline);
// VectorLibraryConfigMerger: when lockPipelineConfig, skips parsing/cleaning/chunking/embedding updates
```

### overrideChunk — preview only

```javascript
// IngestView.vue buildChunkPreviewBody — override applies to chunk-preview request only
const chunkSize = overrideChunkEnabled.value ? overrideChunkSize.value : sizing.chunkSize
// uploadDocument(...) — no chunkSize param passed [ingest.js uploadParams]
```

### 杜鹏飞 anti-pattern fixture (correct settings baseline)

From `ChunkPreviewServiceTest` with `PARAGRAPH_FIRST`, `chunkSize=500`, `chunkOverlap=120`:
- Sample contains 杜鹏飞 weekly-report tab rows + header blocks + Sheet3 tail
- `rawTotalChunks=4`, `filteredOutCount=1`, indexed preview `totalChunks=3`
- Asserts merged continuation lines stay in same chunk (tabular continuation)

### IndexingChunkFilter fallback

```java
// If all chunks header-only, keep originals to avoid zero vectors
return kept.isEmpty() ? List.copyOf(chunks) : kept;
```

## Current Gaps vs Target State (appendix seed)

| Topic | Target (CONTEXT) | Current code | Doc action |
|-------|------------------|--------------|------------|
| Config tiers | System / library default / ingest override | Library-only; ingest tags only | Config matrix with「可覆盖」列 mostly「未实现」 |
| lockPipeline | Soft lock + full-library reindex | Hard lock discards pipeline field updates | Appendix A + link `IndexAdminController.rebuild-library` as partial manual workaround |
| overrideChunk | Preview = ingest | Preview-only; misleading UI label | Flag D-14; Phase 4 PARITY |
| Preview vs index | Same rules | `ParsePreviewService` no normalize; `ChunkPreviewService` re-applies; index reads pre-cleaned `parsed.txt` then cleans again | Side-by-side stage table |
| MIME auto-default | Excel→text-only+paragraph-first, scan PDF→OCR | Wizard defaults OCR off globally; no MIME engine | Describe target; mark backlog |
| Library type wizard | Vertical vs general selection | Wizard has quick/advanced only, no type axis | Decision tree is doc-only until Phase 3 |
| Ingest profile | OCR/chunk override persistence | Not present | Backlog reference |
| tableExtraction structured | Useful for tables | HTML only, not XLSX | Cross-ref Phase 2 TYPE-03 |

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Kafka ingest→index events | `DocumentIndexCoordinator` in-process | MVP monolith | Doc must show synchronous event handoff, not message bus |
| File-extension-based library split | Semantic-axis (target) | Phase 1 decision | Decision tree content |
| Hard pipeline lock | Soft lock + reindex (target) | Not implemented | Gap appendix |

**Deprecated/outdated:**
- `ingestSourceMode: crawl` — upload blocked; historical libraries only.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | User「4 chunk 全召回」uses library params differing from `ChunkPreviewServiceTest` defaults | Common Pitfalls 4 | Doc cites wrong expected count |
| A2 | Mermaid renders in primary doc viewer (GitHub/Cursor) | Standard Stack | Ops cannot view diagrams — provide ASCII fallback in plan |

**Note:** All code-path claims above were verified via codegraph exploration and file reads this session.

## Open Questions (RESOLVED)

1. **INGEST-PIPELINE.md vs split CONFIG-TIERS.md** — **RESOLVED**
   - Decision: Single `INGEST-PIPELINE.md` with TOC anchors `#ops-guide` and `#dev-reference` (Plan 01-T1). Split only if matrix exceeds ~80 rows during execution.
   - Locked in: `01-01-PLAN.md` Task 1 acceptance criteria.

2. **Exact 杜鹏飞 chunk count for ops doc** — **RESOLVED**
   - Decision: Document both「单元测试基准 3 块」（`ChunkPreviewServiceTest`, chunkSize=500）and「运营验证 4 块可召回」with explicit parameter footnote (`chunkSize`, `minParagraphLength`, library config).
   - Locked in: `01-03-PLAN.md` Task 1 anti-pattern sample table.

## Environment Availability

> Step 2.6: SKIPPED for Phase 1 execution (documentation-only). Manual UAT references these dependencies:

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| PostgreSQL + pgvector | Chunk/doc verification examples | ✓ (local infra) | per `init.sql` | — |
| Ollama | Embedding examples in flow | ✓ (typical dev) | per `application.yml` | Doc can describe stage without live embed |
| Tess4J tessdata | OCR anti-pattern sample | ⚠ optional | `infra/tesseract/` | Cite OCR-disabled failure mode from CONCERNS |

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Mockito (`knowbase-service/pom.xml`) |
| Config file | Maven Surefire default |
| Quick run command | `mvn -f knowbase-service/pom.xml test -Dtest=ChunkPreviewServiceTest,IndexingChunkFilterTest` |
| Full suite command | `mvn -f knowbase-service/pom.xml test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PIPE-01 | Config resolver maps library fields | manual doc review | — | ❌ doc not written |
| PIPE-02 | Ingest stages match implemented services | manual doc review + code trace | — | ❌ |
| PIPE-03 | API/class matrix accuracy | manual checklist vs controllers | — | ❌ |
| PIPE-02 (sample) | 杜鹏飞 chunk filter behavior | unit (reference) | `mvn ... -Dtest=ChunkPreviewServiceTest` | ✅ |

### Sampling Rate

- **Per task commit:** Manual section review against resolver map
- **Per wave merge:** Peer read of mermaid + matrix completeness
- **Phase gate:** Success criteria in ROADMAP — newcomer trace exercise

### Wave 0 Gaps

- [ ] `.planning/docs/INGEST-PIPELINE.md` — primary deliverable
- [ ] No automated doc tests — acceptance is manual per ROADMAP

## Security Domain

> Phase 1 documents existing behavior; no auth implementation.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|------------------|
| V2 Authentication | yes (gap) | Document: no Spring Security; tenantId caller-supplied |
| V4 Access Control | yes (gap) | Document: library tenant check only in RAG path |
| V5 Input Validation | yes | Jakarta validation on controllers; MIME allowlist |
| V6 Cryptography | no (ingest doc scope) | SHA-256 checksum only |

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Spoofed tenantId on upload | Spoofing | Document as known limitation; future JWT |
| Oversized upload | DoS | System `max-file-size` + library capacity limits |

## Project Constraints (from .cursor/rules/)

None — `.cursor/rules/` directory not present in workspace.

## Sources

### Primary (HIGH confidence)
- Codegraph exploration — library creation, ingest pipeline, preview/index paths (2026-06-10)
- `knowbase-service/src/main/java/com/knowbase/library/service/LibraryConfigResolver.java`
- `knowbase-service/src/main/java/com/knowbase/ingest/service/DocumentIngestor.java`, `DocumentPipelineService.java`
- `knowbase-service/src/main/java/com/knowbase/vector/service/IndexingService.java`, `ChunkPreviewService.java`
- `frontend/knowbase-ui/src/views/IngestView.vue`, `components/CreateLibraryWizard.vue`

### Secondary (MEDIUM confidence)
- `.planning/codebase/ARCHITECTURE.md`, `CONCERNS.md`, `STRUCTURE.md`
- `.planning/phases/01-ingest-pipeline/01-CONTEXT.md`

### Tertiary (LOW confidence)
- User-reported「4 chunk 全召回」without captured library config snapshot — flagged in Assumptions Log

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — existing codebase, no new packages
- Architecture: HIGH — full path traced with codegraph + file reads
- Pitfalls: HIGH — overrideChunk, lockPipeline, preview/index verified in source

**Research date:** 2026-06-10
**Valid until:** 2026-07-10 (stable brownfield; update if library config model changes)
