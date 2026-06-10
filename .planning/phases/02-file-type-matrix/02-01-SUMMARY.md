---
phase: 02-file-type-matrix
plan: 01
subsystem: docs
tags: [file-type-matrix, config-json, ingest, documentation]

# Dependency graph
requires:
  - phase: 01-ingest-pipeline
    provides: INGEST-PIPELINE.md cross-ref baseline, config tier model, D-01 target-state pattern
provides:
  - FILE-TYPE-PROCESSING.md scaffold with dual TOC, §3 defaults table, §2 matrix skeleton
  - TYPE-01–04 traceability placeholders for Plan 02 fill
affects: [02-file-type-matrix, 03-library-presets, 04-preview-parity]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Dual TOC ops/dev anchors (D-13)"
    - "Three-tier defaults table system/wizard/type-recommend (D-14)"
    - "Single-table matrix skeleton 设定|产出|质量 (D-03/D-04)"

key-files:
  created:
    - .planning/docs/FILE-TYPE-PROCESSING.md
  modified: []

key-decisions:
  - "Excel structured gap deferred to appendix B placeholder; body stays target-state-first (D-01/D-02)"
  - "chunkSize 600 vs 500 and chunkOverlap 100 vs 120 marked as explicit 差异 in §3"
  - "ingest.ocr.enabled distinguished from parsing.ocrEnabled in defaults table"

patterns-established:
  - "Matrix row keys align with libraryConfig.js REINDEX_FIELDS dot-paths"
  - "IndexingChunkFilter rows marked non-config_json in §2 skeleton"

requirements-completed: [TYPE-01, TYPE-02, TYPE-03, TYPE-04]

# Metrics
duration: 12min
completed: 2026-06-10
---

# Phase 2 Plan 01: FILE-TYPE-PROCESSING Scaffold Summary

**Dual-audience doc scaffold with three-tier defaults table and single-table matrix skeleton ready for Plan 02 per-type fill**

## Performance

- **Duration:** 12 min
- **Started:** 2026-06-10T13:26:00Z
- **Completed:** 2026-06-10T13:38:46Z
- **Tasks:** 3
- **Files modified:** 1

## Accomplishments

- Created `FILE-TYPE-PROCESSING.md` with YAML front matter, Phase 2 doc-only banner, and dual TOC (`#ops-guide` / `#dev-reference`)
- §1 scope links INGEST-PIPELINE §2–§4, §5, §7, §8 without duplicating PIPE flow; TYPE-01–05 traceability table present
- §3 three-tier defaults table (9 rule rows) with chunkSize/chunkOverlap diffs and OCR engine vs library distinction
- §2 single-table matrix skeleton covering PDF/Word/Excel/TXT/Markdown with full pipeline row keys and Excel 周报 xlsx sub-scenario placeholder

## Task Commits

Each task was committed atomically:

1. **Task 1: Scaffold doc with dual TOC and PIPE cross-refs** - `a65936d` (docs)
2. **Task 2: Three-tier defaults table (D-14)** - `a26295a` (docs)
3. **Task 3: Main matrix skeleton with pipeline row keys** - `ef0527e` (docs)

**Plan metadata:** `dc99d3a` (docs: complete plan)

## Files Created/Modified

- `.planning/docs/FILE-TYPE-PROCESSING.md` — Phase 2 primary deliverable scaffold (195 lines)

## Decisions Made

- Followed D-01 target-state-first narrative; Excel structured gap referenced via appendix B placeholder only
- §3 cites `application.yml` L152–160, `libraryDefaults.js` L39–73, `ParsingRulesSettings.java`, `VectorLibraryConfigFactory.java` L75–83
- §2 resolver footnote cites `parseOptionsFor` → `DocumentParseService` and `chunkingFor` → `IndexingService` → `IndexingChunkFilter` without duplicating INGEST-PIPELINE §3

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- `git commit` via PowerShell wrapper failed on Co-authored-by trailer parsing; resolved using batch-file commit helper in `.git/`

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02 can fill §2 matrix cells (`（Plan 02 填充）` placeholders) without restructuring
- Plan 03 fills §4, appendices A/B/C, and §9
- STATE.md / ROADMAP.md updates deferred to Plan 02-03 per phase design

## Self-Check: PASSED

- FOUND: `.planning/docs/FILE-TYPE-PROCESSING.md` (195 lines)
- FOUND: `a65936d` Task 1 commit
- FOUND: `a26295a` Task 2 commit
- FOUND: `ef0527e` Task 3 commit

---
*Phase: 02-file-type-matrix*
*Completed: 2026-06-10*
