---
phase: 03-library-presets
plan: 01
subsystem: testing
tags: [vitest, library-presets, junit, appendix-a]

requires:
  - phase: 02-file-type-matrix
    provides: Appendix A MIME expectations and ROADMAP anchors
provides:
  - vitest preset audit suite
  - appendixAPresetAudit.js expectations table
  - VectorLibraryConfigPresetTest JSON roundtrip
affects: [03-library-presets, 04-preview-parity]

tech-stack:
  added: [vitest]
  patterns: [appendix A audit module, ROADMAP anchor assertions]

key-files:
  created:
    - frontend/knowbase-ui/src/utils/appendixAPresetAudit.js
    - frontend/knowbase-ui/src/utils/libraryPresets.test.js
    - knowbase-service/src/test/java/com/knowbase/library/config/VectorLibraryConfigPresetTest.java
  modified:
    - frontend/knowbase-ui/package.json
    - frontend/knowbase-ui/vite.config.js
    - frontend/knowbase-ui/src/utils/libraryPresets.js

key-decisions:
  - "resolveLibraryPresetLabel verifies configMatchesPreset before trusting libraryPresetId"

requirements-completed: [PRESET-01, PRESET-02, PRESET-03]

duration: 12min
completed: 2026-06-10
---

# Phase 3 Plan 01: Preset Audit Tests Summary

**Vitest 18-case preset audit with appendix A/ROADMAP anchors and Java libraryPresetId JSON roundtrip**

## Performance

- **Duration:** 12 min
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- Added vitest to knowbase-ui with node test environment
- Created appendixAPresetAudit.js with APPENDIX_A and ROADMAP anchor expectations
- Full test coverage for 4 presets, applyLibraryPreset contract, and label resolution
- VectorLibraryConfigPresetTest validates libraryPresetId JSON serialization

## Task Commits

1. **Task 1: 附录 A 审计模块 + vitest 预设单测** - `f37a784` (test)
2. **Task 2: 预设对齐修正 + 后端往返测试** - `ede1799` (feat)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] resolveLibraryPresetLabel ignored field drift when libraryPresetId set**
- **Found during:** Task 1 test run
- **Issue:** Changing chunkingStrategy still showed preset name via stale libraryPresetId
- **Fix:** Require configMatchesPreset before returning name from id
- **Files modified:** frontend/knowbase-ui/src/utils/libraryPresets.js
- **Committed in:** ede1799

## Self-Check: PASSED

---
*Phase: 03-library-presets*
*Completed: 2026-06-10*
