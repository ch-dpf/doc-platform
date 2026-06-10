---
phase: 1
slug: ingest-pipeline
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-06-10
---

# Phase 1 — Validation Strategy

> Documentation-only phase. Verification is manual doc review + grep-based task automation.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Manual doc review + `rg` content assertions (no doc test framework) |
| **Config file** | Plan task `<automated>` blocks |
| **Quick run command** | `rg -q "PATTERN" .planning/docs/INGEST-PIPELINE.md` per task |
| **Full suite command** | Plan 03 §9 验收清单 manual walkthrough |
| **Estimated runtime** | ~5 seconds (grep); ~15 min (newcomer trace exercise) |

---

## Sampling Rate

- **After every task commit:** Run task `<automated>` grep checks on `INGEST-PIPELINE.md`
- **After every plan wave:** Review new sections against RESEARCH stage/API matrices
- **Before `/gsd-verify-work`:** Complete §9 验收清单 + newcomer trace exercise (ROADMAP success criteria)
- **Max feedback latency:** 10 seconds (automated grep)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | Status |
|---------|------|------|-------------|-----------|-------------------|--------|
| 01-T1 | 01-01 | 1 | PIPE-01 (scaffold) | grep | `rg -q "#ops-guide" .planning/docs/INGEST-PIPELINE.md` | ⬜ pending |
| 01-T2 | 01-01 | 1 | PIPE-01 (matrix) | grep | `rg -q "系统级" .planning/docs/INGEST-PIPELINE.md` | ⬜ pending |
| 01-T3 | 01-01 | 1 | PIPE-01 (建库) | grep | `rg -q "LibraryConfigResolver" .planning/docs/INGEST-PIPELINE.md` | ⬜ pending |
| 02-T1 | 01-02 | 2 | PIPE-02 | grep | `rg -q "IndexingChunkFilter" .planning/docs/INGEST-PIPELINE.md` | ⬜ pending |
| 02-T2 | 01-02 | 2 | PIPE-03 | grep | `rg -q "DocumentController" .planning/docs/INGEST-PIPELINE.md` | ⬜ pending |
| 02-T3 | 01-02 | 2 | D-14 | grep | `rg -q "overrideChunk" .planning/docs/INGEST-PIPELINE.md` | ⬜ pending |
| 03-T1 | 01-03 | 3 | D-16–D-18 | grep | `rg -q "杜鹏飞" .planning/docs/INGEST-PIPELINE.md` | ⬜ pending |
| 03-T2 | 01-03 | 3 | D-01, D-15 | grep | `rg -q "附录 A" .planning/docs/INGEST-PIPELINE.md` | ⬜ pending |
| 03-T3 | 01-03 | 3 | PIPE-01/02/03 gate | manual | §9 验收清单全部勾选 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red*

---

## Wave 0 Requirements

- [x] `.planning/docs/INGEST-PIPELINE.md` — created incrementally by plans 01–03 (no pre-existing doc required)
- [x] Reference unit tests exist for anti-pattern citation: `ChunkPreviewServiceTest`

*Existing infrastructure covers all phase requirements.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Newcomer trace 建库→document_chunk | ROADMAP success criteria | Subjective comprehension | Reader follows doc without opening code; records stage/class at each step |
| API matrix accuracy | PIPE-03 | Controller paths may drift | Spot-check 5 endpoints against `DocumentController`, `VectorLibraryController` |
| Mermaid diagram correctness | PIPE-02 | No mermaid linter | Visual review of ingest + library-creation flows |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or manual instructions
- [x] Sampling continuity: 9/9 tasks verified per wave
- [x] Wave 0 covers MISSING references (INGEST-PIPELINE.md created in-wave)
- [x] No watch-mode flags
- [x] Feedback latency < 10s (grep)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending execution
