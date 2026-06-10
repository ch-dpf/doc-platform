---
phase: 02-file-type-matrix
verified: 2026-06-10T06:44:59Z
status: passed
score: 15/15 must-haves verified
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 12/15
  gaps_closed:
    - "§2 is a single contiguous 4-column matrix (设定|产出|质量) per D-03/D-04"
    - "Ops/dev anchors and bidirectional cross-doc links resolve to intended sections (D-13, Plan 03 key_links)"
    - "Appendix C accurately aligns with libraryConfig.js REINDEX_FIELDS"
  gaps_remaining: []
  regressions: []
---

# Phase 2: 文件类型设定矩阵 Verification Report

**Phase Goal:** pdf/word/excel/txt/md 各类型的推荐设定、产出形态、风险与反模式。

**Verified:** 2026-06-10T06:44:59Z  
**Status:** passed  
**Re-verification:** Yes — after gap closure (dd50497)

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | ROADMAP: 周报 xlsx、扫描 pdf、制度 docx 各有明确 推荐/禁止 in §2 | ✓ VERIFIED | L88 扫描 pdf **禁止** OCR 关闭; L101 制度 docx **推荐** heading-level / **禁止** fixed-char; L111 周报 **推荐** 专用库 / **禁止** 混库 |
| 2 | Reader can find separate ops and dev entry points (D-13) | ✓ VERIFIED | Dual TOC L15–18; unique anchors `#ops-matrix` L81, `#ops-anti-patterns` L193, `#dev-defaults` L162, `#dev-field-paths` L278; single `{#ops-guide}` at L22 |
| 3 | Target-state-first narrative; Excel structured gap in appendix (D-01, D-02) | ✓ VERIFIED | §1 boundary table L28–39; appendix B L257–274 with POI backlog |
| 4 | Three-tier defaults table maps system/wizard/type-recommend with diff notes (D-14) | ✓ VERIFIED | §3 L172–182: 9 rows; chunkSize 600 vs 500 and chunkOverlap 100 vs 120 marked **差异** |
| 5 | §2 single contiguous matrix with 设定\|产出\|质量 columns and full pipeline row keys (D-03, D-04) | ✓ VERIFIED | L85–124 single 4-col table (PDF→Markdown); dev-reference blocks moved below L128–157 |
| 6 | TYPE-01–04 traceability table present; all Covered | ✓ VERIFIED | §1 L61–67 and §9 L334–340 |
| 7 | PDF rows: OCR, table extraction, scan-PDF 禁止 (TYPE-01) | ✓ VERIFIED | L87–96 |
| 8 | Word rows: structured tables, heading-level for 制度 docx (TYPE-02) | ✓ VERIFIED | L97–104 |
| 9 | Excel rows: text-only, paragraph-first, 周报 sub-scene, structured gap (TYPE-03) | ✓ VERIFIED | L105–112 + appendix B |
| 10 | TXT/Markdown rows: encoding, paragraph chunking (TYPE-04) | ✓ VERIFIED | L113–124 |
| 11 | Appendix A MIME → config_json complete for Phase 3 (D-06, D-07) | ✓ VERIFIED | L227–253: 8 MIME rows |
| 12 | §4 type anti-patterns ≥2 rows per type; link §8 not duplicate (TYPE-05, D-11) | ✓ VERIFIED | §4 L198–212: PDF 3, Word 3, Excel 3, TXT 2, Markdown 3; remediation links use `#ops-matrix` |
| 13 | Bidirectional cross-ref: INGEST-PIPELINE §7.5/§8 → FILE-TYPE-PROCESSING §2/§4 | ✓ VERIFIED | INGEST-PIPELINE L51, L470, L472, L488, L490 target `#ops-matrix` / `#ops-anti-patterns` |
| 14 | Appendix C field paths for dev TOC (D-13) | ✓ VERIFIED | L280 header「主矩阵相关字段子集」; footnotes [^reindex-nc-1]–[^reindex-nc-4] for non-REINDEX_FIELDS; minParagraphLength → application.yml L158 / VectorLibraryConfig.java L291 |
| 15 | §9 acceptance checklist; STATE.md Phase 2 complete | ✓ VERIFIED | §9 L314–330 (7 steps); cross-ref table L369–370 |

**Score:** 15/15 truths verified

### Re-verification Gap Closure (dd50497)

| Gap | Previous | Re-check | Status |
|-----|----------|----------|--------|
| CR-01 §2 table corruption | Dev tables interrupted matrix; Excel/TXT/MD nested in 3-col table | Matrix L85–124 closed before dev blocks L128+ | ✓ CLOSED |
| CR-02 duplicate anchors | 5× `#ops-guide`, 7× `#dev-reference`; PIPE links → §1 | `{#ops-guide}` once (L22); `#ops-matrix`/`#ops-anti-patterns`/`#dev-*` unique; PIPE uses `#ops-matrix` | ✓ CLOSED |
| WR-01/02 Appendix C scope | REINDEX_FIELDS overclaim; wrong minParagraphLength anchor | Subset header + footnotes; minParagraphLength cites application.yml L158 | ✓ CLOSED |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `.planning/docs/FILE-TYPE-PROCESSING.md` | Phase 2 primary deliverable | ✓ VERIFIED | 371 lines; §2 single matrix; unique anchors |
| `.planning/docs/INGEST-PIPELINE.md` | Back-links to Phase 2 | ✓ VERIFIED | §7.5/§8 link to `#ops-matrix` / `#ops-anti-patterns` |
| `.planning/STATE.md` | Phase 2 marked complete | ✓ VERIFIED | Phase 2 Complete |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| FILE-TYPE-PROCESSING §1 | INGEST-PIPELINE §5/§7/§8 | upstream cross-ref | ✓ WIRED | L43–45 |
| FILE-TYPE-PROCESSING §3 | application.yml | §3 系统默认 column | ✓ WIRED | chunkSize 600, chunkOverlap 100 |
| FILE-TYPE-PROCESSING §3 | libraryDefaults.js | §3 向导默认 column | ✓ WIRED | chunkSize 500, chunkOverlap 120 |
| FILE-TYPE-PROCESSING §2 | DocumentParseService.java | PDF/Word footnotes | ✓ WIRED | dev-reference-pdf-word L128–136 |
| FILE-TYPE-PROCESSING §4 | §2 matrix rows | remediation links | ✓ WIRED | All §4 rows link `#ops-matrix` |
| INGEST-PIPELINE §7.5/§8 | FILE-TYPE-PROCESSING §2/§4 | Phase 2 back-link | ✓ WIRED | `#ops-matrix` / `#ops-anti-patterns` |

### Data-Flow Trace (Level 4)

Doc-only phase — no runtime UI/API data flow. §3 default values traced to source files:

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|-------------------|--------|
| §3 defaults table | chunkSize / chunkOverlap | application.yml, libraryDefaults.js | 600/100 vs 500/120 | ✓ FLOWING |
| Appendix A | MIME presets | VectorLibraryConfigFactory FILE_TYPE_MIMES | 8 MIME rows | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| ROADMAP three anchors grep-able | `rg "周报\|扫描\|制度" FILE-TYPE-PROCESSING.md` | 20+ matches with 推荐/禁止 | ✓ PASS |
| §2 single-table structure | Inspect L85–124 | Single 4-col table, no nested dev tables | ✓ PASS |
| Unique ops-guide anchor | `rg "{#ops-guide}" FILE-TYPE-PROCESSING.md` | 1 match (L22) | ✓ PASS |
| PIPE #ops-matrix links | `rg "#ops-matrix" INGEST-PIPELINE.md` | L51, L470, L472, L488, L490 | ✓ PASS |
| TYPE-01–05 Covered | `rg "TYPE-0[1-5].*Covered"` | 10 matches | ✓ PASS |

### Probe Execution

Step 7c: SKIPPED — no probe scripts declared for Phase 2 documentation deliverable.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| TYPE-01 | 02-01–02-06 | PDF OCR, tables, chunking, scan risks | ✓ SATISFIED | §2 L87–96; §4 L199–201 |
| TYPE-02 | 02-01–02-06 | Word structured, heading-level | ✓ SATISFIED | §2 L97–104; §4 L202–204 |
| TYPE-03 | 02-01–02-06 | Excel text-only, paragraph-first | ✓ SATISFIED | §2 L105–112; appendix B |
| TYPE-04 | 02-01–02-06 | TXT/MD encoding, paragraph chunking | ✓ SATISFIED | §2 L113–124 |
| TYPE-05 | 02-03–02-06 | Type-specific misconfig gallery | ✓ SATISFIED | §4 L198–212 |

### Anti-Patterns Found

None blocking. Previous CR-01/CR-02/WR-01/WR-02 resolved in dd50497.

### Human Verification Required

None — structural defects verified from source markdown.

### Gaps Summary

All three initial verification gaps closed in dd50497. Phase 2 goal achieved: substantive file-type matrix, unique dual-audience navigation, accurate Appendix C field subset, and bidirectional INGEST-PIPELINE cross-links.

---

_Verified: 2026-06-10T06:44:59Z_  
_Verifier: Claude (gsd-verifier)_
