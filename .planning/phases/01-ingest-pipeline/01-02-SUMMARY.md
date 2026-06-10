---
phase: 01-ingest-pipeline
plan: 02
subsystem: documentation
tags: [markdown, mermaid, ingest-pipeline, pipe-02, pipe-03, preview-parity]

requires:
  - phase: 01-ingest-pipeline
    plan: 01
    provides: INGEST-PIPELINE.md scaffold, config matrix, PIPE-01 §2
provides:
  - INGEST-PIPELINE.md §3 nine-stage ingest pipeline (PIPE-02)
  - INGEST-PIPELINE.md §4 stage/class/API matrix (PIPE-03)
  - INGEST-PIPELINE.md §4.4 preview vs index gap table (D-14)
affects: [01-03, 04-preview-parity]

tech-stack:
  added: []
  patterns:
    - "Nine-stage D-13 pipeline table with LibraryConfigResolver method per stage"
    - "Dual preview API fork documented separately from upload path"
    - "Side-by-side preview vs index gap table with code anchors"

key-files:
  created: []
  modified:
    - .planning/docs/INGEST-PIPELINE.md

key-decisions:
  - "§4.4 placed under §4 (not §3.5) to keep API matrix and preview gap together"
  - "Preview gap cites IngestView.vue:531 and :1132 plus ingest.js uploadParams as D-14 evidence"

patterns-established:
  - "Stage table columns: # | 阶段 | 关键类 | 配置来源 | 持久化/输出"
  - "API matrix columns: 阶段 | HTTP | Controller | 服务类 | api/*.js | UI 组件"

requirements-completed: [PIPE-02, PIPE-03]

duration: 20min
completed: 2026-06-10
---

# Phase 1 Plan 02: 入库流程与 API 矩阵 Summary

**§3 九阶段入库 mermaid + 阶段表（含 IndexingChunkFilter）；§4 全链路 API/组件矩阵；§4.4 预览≠入库差距表（overrideChunk 仅预览）**

## Performance

- **Duration:** ~20 min
- **Started:** 2026-06-10T12:25:00Z
- **Completed:** 2026-06-10T12:45:00Z
- **Tasks:** 3/3
- **Files modified:** 1

## Accomplishments

- 替换 §3 占位符：sequenceDiagram（IngestView → document_chunk）、9 阶段表、documentMetadata 语义流（D-06）、afterCommit 异步说明
- 填充 §4.1–4.3：11 行 API 矩阵、12 行组件职责表、双预览 API 分叉说明
- 新增 §4.4：预览 vs 入库五维差距表，overrideChunk 代码锚点，Phase 4 PARITY 目标态链接
- INGEST-PIPELINE.md 由 281 行增至 **430 行**

## Task Commits

1. **Task 1: PIPE-02 ingest pipeline flow and stage table** — `3a8686b`
2. **Task 2: PIPE-03 stage/class/API matrix** — `1edf8d0`
3. **Task 3: Preview vs index current gaps (D-14)** — `5e5a2c3`

**Plan metadata:** pending (docs commit via gsd-tools)

## Files Created/Modified

- `.planning/docs/INGEST-PIPELINE.md` — §3 PIPE-02、§4 PIPE-03、§4.4 D-14；需求追溯表 PIPE-02/03 标记完成

## Decisions Made

- §4.4 置于 §4 末尾（与 API 矩阵同章），便于开发从预览分叉直接读到差距表
- 不在 §3 展开反模式/附录 A 细节，交叉引用 §8 与 Plan 03 附录

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- Git 2.31.1 不支持 Cursor 注入的 `--trailer` 参数；改用 `D:\software\git\Git\cmd\git.exe commit -F` 完成提交

## User Setup Required

None - documentation-only phase.

## Next Phase Readiness

- Plan 01-03 可填充 §7/§8 与附录 A/B（质量准则、反模式、四锚点差距详表）
- PIPE-02/03 需求已满足；ROADMAP「新人可追到 document_chunk」部分达成（质量/附录待 Plan 03）

## Self-Check: PASSED

- [x] `.planning/docs/INGEST-PIPELINE.md` exists (430 lines)
- [x] Commits `3a8686b`, `1edf8d0`, `5e5a2c3` present

---
*Phase: 01-ingest-pipeline*
*Completed: 2026-06-10*
