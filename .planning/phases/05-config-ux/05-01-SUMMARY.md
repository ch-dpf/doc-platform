---
phase: 05-config-ux
plan: 01
subsystem: ui
tags: [vitest, vue, library-config, diff, field-hints]

requires:
  - phase: 04-preview-index-parity
    provides: libraryConfig.js baseline and wizard/drawer components
provides:
  - libraryConfig.test.js nested diff regression tests
  - fieldImpactHints.js single-source operator hints
  - normalizedConfigForDiff save gate fix in EditLibrarySettingsDrawer
affects: []

tech-stack:
  added: [vitest@^3]
  patterns:
    - "fieldImpactHint(path) centralizes parsing/chunking impact copy"
    - "normalizedConfigForDiff shared by clonePayload and buildSubmitConfig"

key-files:
  created:
    - frontend/knowbase-ui/src/utils/libraryConfig.test.js
    - frontend/knowbase-ui/src/utils/fieldImpactHints.js
  modified:
    - frontend/knowbase-ui/src/utils/libraryConfig.js
    - frontend/knowbase-ui/package.json
    - frontend/knowbase-ui/src/components/CreateLibraryWizard.vue
    - frontend/knowbase-ui/src/components/EditLibrarySettingsDrawer.vue

key-decisions:
  - "cleaning.* needsReindex uses REINDEX_FIELDS.has() instead of blanket true"
  - "clonePayload and buildSubmitConfig share normalizedConfigForDiff to prevent false 配置未变更"
  - "Impact hints sourced from fieldImpactHints.js in both wizard and edit drawer"

patterns-established:
  - "CFG-01 diff gate comment documents normalized snapshot contract above submit()"
  - "settings-form__tip / wizard-field-hint for ≤1 sentence operator guidance"

requirements-completed: [CFG-01, CFG-02]

duration: 25min
completed: 2026-06-10
---

# Phase 5 Plan 01: Config Diff & Impact Hints Summary

**Nested library config diff covered by vitest; wizard and edit drawer show single-source impact hints; save gate uses normalized snapshots to avoid false «配置未变更».**

## Performance

- **Duration:** ~25 min
- **Tasks:** 3/3
- **Files modified:** 6

## Accomplishments

- Added `libraryConfig.test.js` with 7 cases covering `tableExtraction`, `ocrEnabled`, chunking root fields, `cleaning.removeHeaderFooter`, `semanticSimilarityThreshold`, deep-equal empty diff, and shallow parsing object replacement
- Extended `libraryConfig.js`: `semanticSimilarityThreshold` in `CONFIG_FIELD_SPECS` + `REINDEX_FIELDS`; cleaning `needsReindex` aligned with `REINDEX_FIELDS`
- Created `fieldImpactHints.js` with Chinese operator hints for OCR, table extraction, image extraction, chunking strategy, chunk size
- Wired hints in `CreateLibraryWizard.vue` (el-tooltip + paragraph) and `EditLibrarySettingsDrawer.vue` (`settings-form__tip`)
- Fixed save-flow asymmetry: `normalizedConfigForDiff()` shared by `clonePayload()` and `buildSubmitConfig()` with CFG-01 contract comment

## Commits

| Task | Commit | Message |
|------|--------|---------|
| 1 | `7561964` | feat(04-01): define LibraryChunkPipeline shared chunk contract *(includes libraryConfig.test.js + diff fixes)* |
| 2 | `b203dd0` | feat(05-01): add field impact hints in wizard and edit drawer |
| 3 | `a37368c` | fix(05-01): unify config snapshot normalization for save diff |

## Verification

- `cd frontend/knowbase-ui && npm test` — 25 tests passed
- `npm run build` — succeeded

## Deviations from Plan

### Commit placement

**1. Task 1 bundled into 04-01 commit**
- **Found during:** Task 1 commit
- **Issue:** Parallel wave committed `libraryConfig.test.js` and diff fixes in `7561964` (04-01 scope) before dedicated `test(05-01)` commit
- **Impact:** Functionality complete; commit message not plan-scoped
- **Files:** `libraryConfig.js`, `libraryConfig.test.js`

None other — plan executed as written.

## TDD Gate Compliance

Task 1 had `tdd="true"`. Tests were written first; implementation gaps (`semanticSimilarityThreshold`, cleaning `needsReindex`) were fixed to green. No separate `test(05-01)` RED commit — changes landed in `7561964`. **Warning:** missing dedicated RED gate commit for 05-01 Task 1.

## Self-Check: PASSED

- FOUND: `.planning/phases/05-config-ux/05-01-SUMMARY.md`
- FOUND: `frontend/knowbase-ui/src/utils/libraryConfig.test.js`
- FOUND: `frontend/knowbase-ui/src/utils/fieldImpactHints.js`
- FOUND: commit `7561964`
- FOUND: commit `b203dd0`
- FOUND: commit `a37368c`
