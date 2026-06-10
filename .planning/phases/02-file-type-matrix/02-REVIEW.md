---
phase: 02-file-type-matrix
reviewed: 2026-06-10T16:30:00Z
depth: standard
files_reviewed: 2
files_reviewed_list:
  - .planning/docs/FILE-TYPE-PROCESSING.md
  - .planning/docs/INGEST-PIPELINE.md
findings:
  critical: 2
  warning: 7
  info: 2
  total: 11
status: issues_found
---

# Phase 2: Code Review Report

**Reviewed:** 2026-06-10T16:30:00Z
**Depth:** standard
**Files Reviewed:** 2
**Status:** issues_found

## Summary

Phase 2 is a documentation-only deliverable. Review scoped `FILE-TYPE-PROCESSING.md` (primary) and `INGEST-PIPELINE.md` (bidirectional cross-ref updates) against the live codebase: `application.yml`, `libraryDefaults.js`, `libraryConfig.js`, `VectorLibraryConfigFactory.java`, `DocumentParseService.java`, `ParsingRulesSettings.java`, and related anchors cited in the matrix.

**TYPE-01–05** traceability tables and `REQUIREMENTS.md` completion markers are present and substantively covered. **Three-tier defaults** (`chunkSize` 600/500, `chunkOverlap` 100/120) match code. **ROADMAP three anchors** (周报 xlsx, 扫描 pdf, 制度 docx) are grep-able with 推荐/禁止 in §2.

**Key defects:** §2主矩阵 violates its own「单张大表」contract — Excel/TXT/Markdown rows are embedded inside the Excel dev-reference table, corrupting markdown rendering. Duplicate `#ops-guide` / `#dev-reference` anchor IDs break intra-doc and INGEST-PIPELINE cross-links to §2 vs §4. Appendix C misstates `REINDEX_FIELDS` alignment and one code anchor.

## Critical Issues

### CR-01: §2 main matrix rows embedded inside dev-reference table

**File:** `.planning/docs/FILE-TYPE-PROCESSING.md:118-147`
**Issue:** D-03 promises a single contiguous matrix table. After Word rows (L104), dev-reference subsections (L106–127) interrupt the table. Excel/TXT/Markdown matrix rows (L128–147) use **four** columns (主矩阵 format) but are appended as rows inside the **three-column** Excel dev table that starts at L120. Markdown renderers will produce a broken table; ops cannot reliably read Excel→Markdown rows as one matrix.
**Fix:** Close the §2 main matrix table after Word rows (or move all five types into one uninterrupted table). Relocate dev-reference blocks (PDF/Word, Excel) **below** the closed matrix. Ensure Excel/TXT/Markdown rows use the same 4-column header as PDF/Word (L85–86), not the 3-column dev table.

```markdown
| **Word** · `IndexingChunkFilter` | — | … | … |

### 开发参考：PDF / Word 解析锚点 {#dev-reference-pdf-word}
…

### 开发参考：Excel 解析与分块锚点 {#dev-reference-excel}
| 路径 | 行为 | 矩阵关联 |
| `DocumentParseService.java` L74–77 | … | … |

## §2 主矩阵（续）— 或保持单表不分节
| **Excel** · `parsing.tableExtraction` | … | … | … |
```

### CR-02: Duplicate `#ops-guide` / `#dev-reference` anchors break cross-refs

**File:** `.planning/docs/FILE-TYPE-PROCESSING.md:22,81,193,314` and `.planning/docs/INGEST-PIPELINE.md:51,470,488,490`
**Issue:** `#ops-guide` is assigned to §1, §2, §4, and §9; `#dev-reference` to §1, §3, dev subsections, appendices, and §9. HTML/markdown renderers resolve duplicate IDs to the **first** occurrence (§1). Links such as INGEST-PIPELINE「§4 类型反模式」→ `FILE-TYPE-PROCESSING.md#ops-guide`, §9 step 4「阅读 §4」→ `#ops-guide`, and §4「链回 §2 主矩阵」→ `#ops-guide` all land on §1, not §2 or §4. The §9 acceptance checklist step 6 cannot be executed as written.
**Fix:** Use unique anchors per section, e.g. `{#ops-matrix}`, `{#ops-anti-patterns}`, `{#dev-defaults}`, `{#dev-field-paths}`. Update INGEST-PIPELINE links:

```markdown
[§2 主矩阵](./FILE-TYPE-PROCESSING.md#ops-matrix)
[§4 类型反模式](./FILE-TYPE-PROCESSING.md#ops-anti-patterns)
```

## Warnings

### WR-01: Appendix C falsely claims full `REINDEX_FIELDS` alignment

**File:** `.planning/docs/FILE-TYPE-PROCESSING.md:280,282-295`
**Issue:** Intro states alignment with `REINDEX_FIELDS` (L35–53). Four listed fields are **not** in `REINDEX_FIELDS`: `parsing.autoDetectEncoding`, `parsing.defaultLanguage`, `cleaning.removeHeaderFooter`, and `ingestAccess.supportedFileTypes` (correctly marked 否 for reindex, but still not in the Set). Downstream Phase 5 CFG-01 consumers may assume a complete REINDEX_FIELDS mapping.
**Fix:** Either add missing parsing/cleaning fields to `REINDEX_FIELDS` in code (if intended), or revise Appendix C header to「主矩阵相关字段（含 `diffLibraryConfig` 追踪但不在 `REINDEX_FIELDS` 者标注说明）」and footnote the four exceptions.

### WR-02: `minParagraphLength` system anchor cites wrong class/lines

**File:** `.planning/docs/FILE-TYPE-PROCESSING.md:291`
**Issue:** Appendix C lists code anchor `VectorLibraryConfigFactory.java` L75–83 for `minParagraphLength`. Factory L75–83 only defaults `chunkingStrategy`, `chunkSize` (600), and `chunkOverlap` (100) — **not** `minParagraphLength`. System default 30 comes from `VectorLibraryConfig.java` field default and `application.yml` `chunking.min-paragraph-length: 30` (L158).
**Fix:**

```markdown
| `minParagraphLength` | … | `application.yml` L158；`VectorLibraryConfig.java` 字段默认 30 |
```

### WR-03: Appendix A Word preset inconsistent with §2 tier defaults

**File:** `.planning/docs/FILE-TYPE-PROCESSING.md:234` vs `101`
**Issue:** Appendix A recommends `{ chunkingStrategy: 'heading-level' }` for all Word MIME rows. §2 Word row says 制度 docx → `heading-level`; **短文** → `paragraph-first`. Phase 3 `libraryPresets.js` consuming Appendix A verbatim would over-apply heading-level to short documents.
**Fix:** Split Word appendix rows or add fragment note: `heading-level`（制度/长文）；`paragraph-first`（短文默认），与 §2 Word chunkingStrategy 行一致.

### WR-04: INGEST-PIPELINE cannot link to FILE-TYPE §4 distinctly from §2

**File:** `.planning/docs/INGEST-PIPELINE.md:51,470,488,490`
**Issue:** All FILE-TYPE-PROCESSING backlinks use `#ops-guide`. §7.5「类型专属反模式见 §4」and §8「链回 §4」resolve to §1 (duplicate anchor). TYPE-05 coverage is not navigable from PIPE as documented.
**Fix:** After CR-02 anchor fix, update §4 links to `#ops-anti-patterns`.

### WR-05: Markdown MIME whitelist incomplete in §2 and Appendix A

**File:** `.planning/docs/FILE-TYPE-PROCESSING.md:143,238-240`
**Issue:** §2 Markdown MIME row and Appendix A cite `text/markdown`, `text/x-markdown` only. Platform whitelist (`application.yml` L90–92, `IngestProperties`, `ChunkMetadataBuilder`) also includes **`text/x-web-markdown`**. Ops following the doc may omit this MIME when reasoning about upload acceptance.
**Fix:** Add `text/x-web-markdown` to §2 Markdown MIME row and Appendix A (可合并入 `text/x-markdown` 行并脚注「含 text/x-web-markdown」).

### WR-06: `DocumentParseService` L95–107 cited as xlsx-specific path

**File:** `.planning/docs/FILE-TYPE-PROCESSING.md:122,205`
**Issue:** Lines 95–107 are generic `extractPlainWithTika` — no xlsx branch. Excel「structured 不生效」behavior follows from L74–77 (`requiresHtmlPipeline()` false for typical xlsx) plus architecture, not L95–107 specifically. Misleading anchor for developers tracing Excel parse path.
**Fix:** Cite `DocumentParseService.java` L74–77 + `TableExtractionMode` / HTML-pipeline scope; note xlsx never enters HTML pipeline regardless of `structured` setting.

### WR-07: Appendix A link to INGEST-PIPELINE「附录 B.3」lacks fragment

**File:** `.planning/docs/FILE-TYPE-PROCESSING.md:253,265`
**Issue:** Links use `./INGEST-PIPELINE.md` without `#` anchor to B.3. Readers land at document top, not「结构化双轨 + QueryRouter」backlog row.
**Fix:** Add explicit heading anchor in INGEST-PIPELINE B.3 (e.g. `{#b3-deferred}`) and link `[附录 B.3](./INGEST-PIPELINE.md#b3-deferred)`.

## Info

### IN-01: `TableExtractionMode` line reference off by one

**File:** `.planning/docs/FILE-TYPE-PROCESSING.md:123`
**Issue:** Doc cites L8–9 for `STRUCTURED` enum; `STRUCTURED` is defined at L9 (`TEXT_ONLY` at L8). Low impact but imprecise for dev TOC.
**Fix:** Cite L9 or「L8–10 enum 定义区」.

### IN-02: §3「系统默认」conflates YAML and Java class defaults

**File:** `.planning/docs/FILE-TYPE-PROCESSING.md:168-186`
**Issue:** §3 attributes chunking defaults to `application.yml` L152–160. Runtime `LibraryConfigResolver.chunkingFor` reads **`config_json`** (`VectorLibraryConfig`); YAML `chunking.*` applies via `globalChunkingDefaults` mainly for `semanticSimilarityThreshold` fallback (L112–114). Narrative is directionally correct for wizard vs兜底 but blurs two system sources.
**Fix:** Add footnote:「向导建库写入 config_json；未设字段时 `VectorLibraryConfig` Java 默认 + Factory ≤0 兜底；YAML chunking.* 为全局 ChunkingProperties 注入源。」

---

_Reviewed: 2026-06-10T16:30:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
