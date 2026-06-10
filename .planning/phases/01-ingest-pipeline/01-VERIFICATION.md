---
phase: 01-ingest-pipeline
verified: 2026-06-10T15:00:00Z
status: passed
score: 14/14 must-haves verified
overrides_applied: 0
human_verification:
  - test: "Follow §9 验收清单五步追溯，全程不打开 Java/Vue 源码"
    expected: "每步可通过文档内章节锚点与表格独立完成：建库字段→config_json→上传 API→九阶段管道→document_chunk 写入；仅在 §9 提示处理解预览≠入库"
    why_human: "ROADMAP 成功标准要求「新人无歧义追溯」— 结构完整性可 grep 验证，但实际可读性与无歧义性需人读"
  - test: "在 Markdown 预览中打开 INGEST-PIPELINE.md，检查三处 mermaid 图（§2.1 flowchart LR、§3.1 sequenceDiagram、§6.1 flowchart TD）"
    expected: "三图均正常渲染，无 syntax error；决策树节点文字可读"
    why_human: "Mermaid 语法存在不等于所有渲染器正确显示；grep 无法验证视觉布局"
---

# Phase 1: 全链路流程梳理 Verification Report

**Phase Goal:** 可读文档描述建库与入库端到端路径，开发与运营对齐「设定在哪生效」。
**Verified:** 2026-06-10T15:00:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Reader can find separate ops and dev entry points | ✓ VERIFIED | §1 目录表 `#ops-guide` / `#dev-reference` 双锚点；grep 无占位符 |
| 2 | Target-state-first narrative with v1 scope boundary (D-02) | ✓ VERIFIED | §1「目标态 vs v1 交付边界」表；D-01/D-02 明示；RAG 北极星非 v1 工程验收 |
| 3 | Library type decision tree by semantic axis (D-08–D-12) | ✓ VERIFIED | §6 mermaid flowchart TD + §6.2 周报/报销启发式（D-10、D-11） |
| 4 | Three-tier config matrix (D-07) | ✓ VERIFIED | §5.1 16 行矩阵，四列（系统/库默认/采集覆盖/现状）+ 必须/默认/可覆盖/禁止 单元格 |
| 5 | PIPE-01: wizard → config_json → LibraryConfigResolver | ✓ VERIFIED | §2.2 向导字段表、`WIZARD_STEPS` 与 `libraryDefaults.js` 对齐；§2.3 resolver 表；§2.1 mermaid |
| 6 | PIPE-02: upload → document_chunk full path | ✓ VERIFIED | §3.1 sequenceDiagram + §3.2 九阶段表；末阶段 `INSERT document_chunk` |
| 7 | PIPE-03: stage × class × API matrix | ✓ VERIFIED | §4.1 11 行 API 矩阵 + §4.2 12 行组件表；前后端列齐全 |
| 8 | Preview vs index divergence (D-14) | ✓ VERIFIED | §4.4 五维差距表；`overrideChunk` 仅预览；代码锚点与 `IngestView.vue:531/1132`、`ingest.js uploadParams` 一致 |
| 9 | D-13 stages name DocumentPipelineService, IndexingService, IndexingChunkFilter | ✓ VERIFIED | §3.2 阶段 2/6/7/8 及 §3.1 时序图参与者 |
| 10 | RAG north star + v1 recall acceptance (D-16) | ✓ VERIFIED | §7.1 北极星 vs §7.2 v1 检索可召回分层；§7.3 IndexingChunkFilter 角色 |
| 11 | Anti-pattern gallery (D-18) | ✓ VERIFIED | §8 5 行：预览≠入库、杜鹏飞 xlsx、扫描 PDF OCR、异质混库、Excel structured |
| 12 | Appendix A four mandatory anchors | ✓ VERIFIED | 附录 A 摘要表 + A.1–A.4 展开：DPS、IndexingService、IngestView、VectorLibraryConfigMerger |
| 13 | Appendix B backlog (D-19, deferred) | ✓ VERIFIED | 附录 B：Phase 2–5 映射、GATE-01/02、ingest profile、MIME、软锁 backlog |
| 14 | Newcomer trace 建库 → document_chunk (ROADMAP SC) | ✓ VERIFIED | §9 五步验收清单，每步有章节锚点；PIPE-01/02/03 终稿追溯表 Covered |

**Score:** 14/14 truths verified (automated); human UAT pending for §9 readability

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `.planning/docs/INGEST-PIPELINE.md` | Full Phase 1 doc ≥400 lines | ✓ VERIFIED | 659 lines; front matter `last_mapped_commit: ff9f059`; 无「Plan 02/03 填充」占位 |
| `.planning/STATE.md` | Phase 1 marked complete | ✓ VERIFIED | Phase 1 row: `Complete（文档已交付）` |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| INGEST-PIPELINE.md §2 | CreateLibraryWizard.vue | wizard ↔ config_json table | ✓ WIRED | §2.2 五步映射；`WIZARD_STEPS` 存在于 `libraryDefaults.js` |
| INGEST-PIPELINE.md §2 | LibraryConfigResolver.java | *For resolver map | ✓ WIRED | §2.3/§5.2 方法名与源码 `parseOptionsFor/chunkingFor/cleaningFor/embeddingFor` 一致 |
| INGEST-PIPELINE.md §3 | DocumentIngestor.java | upload stage | ✓ WIRED | §3.2 阶段 1；`scheduleProcessAfterCommit` 在 `DocumentIngestor.java:179` |
| INGEST-PIPELINE.md §4 | IngestView.vue | API matrix UI column | ✓ WIRED | §4.1/4.2；预览/上传路径与组件引用一致 |
| INGEST-PIPELINE.md §4 | IndexingService.java | chunk → filter → embed | ✓ WIRED | §3.2 阶段 6–8；`IndexingService.java:153–158` 与文档引用一致 |
| INGEST-PIPELINE.md §8 | ChunkPreviewServiceTest.java | 杜鹏飞 anti-pattern | ✓ WIRED | 测试 `rawTotalChunks=4, filteredOutCount=1, totalChunks=3` 与文档脚注一致 |
| INGEST-PIPELINE.md 附录 A | VectorLibraryConfigMerger.java | lockPipeline hard lock | ✓ WIRED | `mergeSafeFields` early return at line 67–69 与文档代码块一致 |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|-------------------|--------|
| §2.2 wizard table | config_json paths | `libraryDefaults.js` `defaultLibraryConfig` | ✓ | ✓ FLOWING — 字段路径与源码 export 对齐 |
| §4.1 API matrix | HTTP paths | Controller `@RequestMapping` | ✓ | ✓ FLOWING — `DocumentController` `/upload`, `/parse-preview`; `IndexAdminController` `/chunk-preview`, `/rebuild-library` 核对通过 |
| §4.4 preview gap | overrideChunk | `IngestView.vue` + `ingest.js` | ✓ | ✓ FLOWING — override 仅进 `buildChunkPreviewBody`; `uploadParams` 不传 chunkSize |
| §8 杜鹏飞行 | chunk counts | `ChunkPreviewServiceTest` | ✓ | ✓ FLOWING — assert 4/1/3 与文档一致 |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Doc ≥400 lines, no placeholders | grep `#ops-guide`; no `Plan 02/03 填充` | 659 lines; dual anchors present | ✓ PASS |
| API upload path exists | `DocumentController` `@PostMapping("/upload")` | Matches `POST /api/v1/documents/upload` | ✓ PASS |
| 杜鹏飞 fixture test exists | `ChunkPreviewServiceTest.previewUsesIndexingChunkFilterAndLibraryChunkParams` | Test file with 4/1/3 asserts | ✓ PASS |
| Three mermaid diagrams | grep `flowchart\|sequenceDiagram` | 3 matches (§2.1, §3.1, §6.1) | ✓ PASS |
| Config matrix tier vocabulary | grep 必须/默认/可覆盖/禁止 | 61 occurrences | ✓ PASS |

### Probe Execution

Step 7c: SKIPPED — Phase 1 is documentation-only; no `scripts/*/tests/probe-*.sh` declared or required.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| PIPE-01 | 01-01, 01-03 | 建库流程 wizard → config_json → resolver | ✓ SATISFIED | §2 完整；REQUIREMENTS.md traceability Covered |
| PIPE-02 | 01-02, 01-03 | 单文档入库全流程至 document_chunk | ✓ SATISFIED | §3 九阶段 + sequenceDiagram |
| PIPE-03 | 01-02, 01-03 | 阶段关键类与 API 对照 | ✓ SATISFIED | §4.1–4.2 矩阵 ≥8 API 行、≥10 组件行 |

No orphaned Phase 1 requirements in REQUIREMENTS.md beyond PIPE-01/02/03.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| — | — | None | — | No TBD/FIXME/TODO or placeholder stubs in phase deliverables |

### Human Verification Required

### 1. §9 Newcomer Trace Exercise

**Test:** 按 §9 验收清单五步追溯，不打开 `knowbase-service/` 或 `frontend/knowbase-ui/` 源码。
**Expected:** 每步仅通过 INGEST-PIPELINE.md 内锚点与表格即可完成理解；第 5 步明确预览块数≠入库块数。
**Why human:** ROADMAP 成功标准「无歧义」需人读验证；自动化仅能确认清单结构与锚点存在。

### 2. Mermaid Diagram Rendering

**Test:** 在 IDE 或文档站点预览 INGEST-PIPELINE.md，检查 §2.1、§3.1、§6.1 三处 mermaid。
**Expected:** 三图正常渲染，决策树节点与流程箭头可读。
**Why human:** grep 可证语法块存在，无法验证渲染器兼容性。

### Gaps Summary

No automated gaps. All 14 must-have truths verified against document content and codebase cross-references. Phase goal achievement pending human confirmation of §9 newcomer trace and mermaid rendering.

---

_Verified: 2026-06-10T15:00:00Z_
_Verifier: Claude (gsd-verifier)_
