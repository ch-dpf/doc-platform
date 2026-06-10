---
phase: 02-file-type-matrix
plan: 04
subsystem: docs
tags: [file-type-matrix, matrix-structure, gap-closure, documentation, CR-01]

# Dependency graph
requires:
  - phase: 02-file-type-matrix
    plan: 03
    provides: §2 matrix content, §4 anti-patterns, appendices A/B/C
provides:
  - §2 single contiguous 4-column ops matrix per D-03/D-04
  - Dev-reference blocks relocated below closed matrix (CR-01 closed)
  - Excel/TXT/Markdown rows in same table as PDF/Word
affects: [02-file-type-matrix plan 05 (CR-02 anchors), 03-library-presets]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "§2 D-03 single-table contract: one 4-column header, all five type row groups contiguous, dev-reference 3-column tables below matrix"

key-files:
  created: []
  modified:
    - .planning/docs/FILE-TYPE-PROCESSING.md

key-decisions:
  - "Option A (D-03 单张大表): one uninterrupted 4-column matrix through Markdown rows before dev-reference blocks"
  - "D-08 Excel target-state footnote kept outside main matrix, between matrix end and dev-reference subsections"

patterns-established:
  - "Dev-reference code-anchor tables never embed 4-column matrix data rows"

requirements-completed: [TYPE-01, TYPE-02, TYPE-03, TYPE-04]

# Metrics
duration: 10min
completed: 2026-06-10
---

# Phase 2 Plan 04: §2 Matrix Structure Gap Closure Summary

**§2 restored to D-03 single contiguous 4-column matrix — dev-reference blocks moved below Excel/TXT/Markdown rows, closing CR-01**

## Performance

- **Duration:** 10 min
- **Started:** 2026-06-10T18:00:00Z
- **Completed:** 2026-06-10T18:10:00Z
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments

- Closed §2 main matrix corruption: PDF through Markdown now share one 4-column table (38 data rows)
- Relocated `### 开发参考：PDF / Word 解析锚点` and `### 开发参考：Excel 解析与分块锚点` below the closed matrix
- Removed Excel/TXT/Markdown matrix rows from inside the 3-column Excel dev-reference table
- Preserved D-08 Excel target-state footnote outside the matrix table (between matrix end and dev blocks)
- ROADMAP anchors (周报 xlsx, 扫描 pdf, 制度 docx) unchanged with 推荐/禁止 labels

## Task Commits

Each task was committed atomically:

1. **Task 1: Close §2 matrix after Word rows and relocate dev-reference blocks** - `08e5500` (docs)
2. **Task 2: Validate matrix row coverage and markdown table integrity** - validation only (no file diff; structure verified)

**Plan metadata:** pending (this commit)

## Files Created/Modified

- `.planning/docs/FILE-TYPE-PROCESSING.md` — §2 L81–159 restructured; single matrix header L85; contiguous rows L87–124; dev blocks L128–158

## Matrix Row Inventory (Task 2 spot-check)

| Type | Rows in §2 | Plan minimum | Status |
|------|------------|--------------|--------|
| PDF | 10 | ≥8 | ✓ |
| Word | 8 | ≥7 | ✓ |
| Excel | 8 | ≥8 (incl. 周报 sub-scene) | ✓ |
| TXT | 6 | ≥5 | ✓ |
| Markdown | 6 | ≥5 | ✓ |

§2 matrix section: 79 lines (L81–159), ≥55 required.

## Decisions Made

- Used Option A (single uninterrupted table) per D-03 — dev blocks entirely below Markdown's last row
- Left anchor IDs unchanged (`{#dev-reference}` duplicates) — Plan 02-05 handles CR-02

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- `git commit` wrapper failed on `--trailer` option; resolved using `git.exe commit -F` message file pattern (same as Plan 02-03)
- PowerShell automated verify script failed on encoding/regex; manual grep verification confirmed acceptance criteria

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02-05 can fix CR-02 duplicate anchor IDs (`#ops-guide`, `#dev-reference`)
- Plan 02-06 can address Appendix C REINDEX_FIELDS overclaim (WR-01, WR-02)
- Verification truth #5 (single contiguous matrix) now achievable after re-verification

## Self-Check: PASSED

- FOUND: `.planning/docs/FILE-TYPE-PROCESSING.md` (§2 restructured)
- FOUND: `.planning/phases/02-file-type-matrix/02-04-SUMMARY.md`
- FOUND: `08e5500` Task 1 commit
- VERIFIED: Exactly one `| 类型 × 规则项 |` header in §2
- VERIFIED: No `### 开发参考` between Word L104 and Excel L105
- VERIFIED: Excel/TXT/Markdown rows have 4 pipe-delimited columns
- VERIFIED: ROADMAP anchors 周报/扫描/制度 grep-able with 推荐/禁止

---
*Phase: 02-file-type-matrix*
*Completed: 2026-06-10*
