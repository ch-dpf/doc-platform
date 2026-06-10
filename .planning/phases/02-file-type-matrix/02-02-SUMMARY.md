---
phase: 02-file-type-matrix
plan: 02
subsystem: docs
tags: [file-type-matrix, mime-mapping, excel-gap, ingest, documentation]

# Dependency graph
requires:
  - phase: 02-file-type-matrix
    plan: 01
    provides: FILE-TYPE-PROCESSING.md scaffold with §2 skeleton and §3 defaults table
provides:
  - §2 main matrix filled for pdf/word/excel/txt/markdown (TYPE-01–04)
  - Appendix A MIME → config_json mapping (D-06/D-07)
  - Appendix B structured Excel gap and backlog (D-08–D-10)
  - ROADMAP anchors for 周报 xlsx, 扫描 pdf, 制度 docx
affects: [02-file-type-matrix, 03-library-presets, 04-preview-parity]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Per-type 设定|产出|质量 matrix cells with 推荐/禁止 vocabulary"
    - "Dev code-anchor tables under §2 for DocumentParseService and Excel pipeline"
    - "Appendix A Phase 3 preset banner with D-07 three-tier override narrative"

key-files:
  created: []
  modified:
    - .planning/docs/FILE-TYPE-PROCESSING.md

key-decisions:
  - "Excel structured explicitly 禁止 in §2 with Appendix B cross-ref; no POI v1 promise (D-10)"
  - "Scan PDF split into text-type vs scan-type ocrEnabled rows with OcrFallbackPolicy 32-char threshold"
  - "Appendix A deferred as planning table only — not v1 runtime MIME auto-default engine (D-06)"

patterns-established:
  - "ROADMAP scenario anchors embedded in matrix 质量 column with bold 推荐/禁止"
  - "Excel D-08 target-state one-liner before dev footnotes; transition path in Appendix B"

requirements-completed: [TYPE-01, TYPE-02, TYPE-03, TYPE-04]

# Metrics
duration: 18min
completed: 2026-06-10
---

# Phase 2 Plan 02: File-Type Matrix Fill Summary

**§2 ops matrix with scan-PDF OCR rules, 制度 docx heading-level, 周报 xlsx transition path, and Appendix A MIME presets for Phase 3**

## Performance

- **Duration:** 18 min
- **Started:** 2026-06-10T14:00:00Z
- **Completed:** 2026-06-10T14:18:00Z
- **Tasks:** 3
- **Files modified:** 1

## Accomplishments

- Filled all §2 matrix cells for PDF, Word, Excel, TXT, and Markdown — no remaining `（Plan 02 填充）` placeholders
- ROADMAP three anchors present: **扫描 pdf** OCR 禁止关闭, **制度 docx** structured + heading-level, **周报 xlsx** homogeneous library 推荐/禁止
- Wrote **附录 A** MIME → config_json table (8 MIME rows) with Phase 3 preset banner and D-07 three-tier override narrative
- Wrote **附录 B** structured Excel gap table (目标态/v1/过渡推荐/Backlog) with PROJECT Out of Scope and CONCERNS cross-refs

## Task Commits

Each task was committed atomically:

1. **Task 1: PDF and Word matrix rows (TYPE-01, TYPE-02)** - `52c2233` (docs)
2. **Task 2: Excel matrix rows and Appendix B (TYPE-03)** - `48647bd` (docs)
3. **Task 3: TXT/Markdown rows and Appendix A (TYPE-04)** - `be64583` (docs)

**Plan metadata:** `885a5c0` (docs: complete plan)

## Files Created/Modified

- `.planning/docs/FILE-TYPE-PROCESSING.md` — §2 complete (259 lines); 附录 A/B filled; §3 synced for MD heading-level

## Decisions Made

- Split PDF `parsing.ocrEnabled` into text-type vs scan-type sub-rows within single matrix table (clearer ROADMAP anchor than combined cell)
- Appendix B placed in Plan 02 (not deferred to Plan 03) per plan task 2 action — Plan 03 retains §4/附录 C/§9 only
- D-12 杜鹏飞 fixture cited as optional footnote in Excel chunkSize row and dev anchor table

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- PowerShell does not support `&&` for chained git commands; used semicolon separators instead

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02-03 can update STATE.md / ROADMAP.md and mark requirements complete
- Plan 03 can fill §4 type anti-patterns, 附录 C field paths, and §9 acceptance checklist
- Phase 3 `libraryPresets.js` can consume Appendix A MIME fragments

## Self-Check: PASSED

- FOUND: `.planning/docs/FILE-TYPE-PROCESSING.md` (259 lines)
- FOUND: `52c2233` Task 1 commit
- FOUND: `48647bd` Task 2 commit
- FOUND: `be64583` Task 3 commit
- VERIFIED: No `（Plan 02 填充）` placeholders in §2
- VERIFIED: ROADMAP anchors 周报 xlsx / 扫描 pdf / 制度 docx grep-able with 推荐/禁止

---
*Phase: 02-file-type-matrix*
*Completed: 2026-06-10*
