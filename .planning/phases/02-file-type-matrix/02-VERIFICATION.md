---
phase: 02-file-type-matrix
verified: 2026-06-10T20:00:00Z
status: passed
score: 15/15 must-haves verified
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 12/15
  gaps_closed:
    - "§2 is a single contiguous 4-column matrix (设定|产出|质量) per D-03/D-04 — CR-01 closed in 02-04"
    - "Ops/dev anchors and bidirectional cross-doc links resolve to intended sections — CR-02 closed in 02-05"
    - "Appendix C accurately aligns with libraryConfig.js REINDEX_FIELDS scope — WR-01/WR-02 closed in 02-06"
  gaps_remaining: []
  regressions: []
---

# Phase 2: 文件类型设定矩阵 Verification Report

**Phase Goal:** pdf/word/excel/txt/md 各类型的推荐设定、产出形态、风险与反模式。

**Verified:** 2026-06-10T20:00:00Z  
**Status:** passed  
**Re-verification:** Yes — after gap closure plans 02-04, 02-05, 02-06

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | ROADMAP: 周报 xlsx、扫描 pdf、制度 docx 各有明确 推荐/禁止 in §2 | ✓ VERIFIED | L88 扫描 pdf **禁止** OCR 关闭; L101 制度 docx **推荐** heading-level / **禁止** fixed-char; L111 周报 **推荐** 专用库 / **禁止** 混库 |
| 2 | Reader can find separate ops and dev entry points (D-13) | ✓ VERIFIED | TOC L15–18 links `#ops-matrix`, `#ops-anti-patterns`, `#dev-defaults`, `#dev-field-paths`; `{#ops-guide}`/`{#dev-reference}` only on §1 L22 |
| 3 | Target-state-first narrative; Excel structured gap in appendix (D-01, D-02) | ✓ VERIFIED | §1 boundary table L28–39; appendix B L257–274 with POI backlog |
| 4 | Three-tier defaults table maps system/wizard/type-recommend with diff notes (D-14) | ✓ VERIFIED | §3 L172–182: 9 rows; chunkSize 600 vs 500 and chunkOverlap 100 vs 120 marked **差异**; matches `application.yml` L154–155 and `libraryDefaults.js` L40–41 |
| 5 | §2 single contiguous matrix with 设定\|产出\|质量 columns and full pipeline row keys (D-03, D-04) | ✓ VERIFIED | One header L85; 38 contiguous data rows L87–124 (PDF→Word→Excel→TXT→Markdown); dev-reference blocks start L128 after matrix closes |
| 6 | TYPE-01–04 traceability table present; all Covered | ✓ VERIFIED | §1 L61–67 and §9 L341–347 |
| 7 | PDF rows: OCR, table extraction, scan-PDF 禁止 (TYPE-01) | ✓ VERIFIED | L87–96 |
| 8 | Word rows: structured tables, heading-level for 制度 docx (TYPE-02) | ✓ VERIFIED | L97–104 |
| 9 | Excel rows: text-only, paragraph-first, 周报 sub-scene, structured gap (TYPE-03) | ✓ VERIFIED | L105–112 content + appendix B; matrix structure intact |
| 10 | TXT/Markdown rows: encoding, paragraph chunking (TYPE-04) | ✓ VERIFIED | L113–124 |
| 11 | Appendix A MIME → config_json complete for Phase 3 (D-06, D-07) | ✓ VERIFIED | L227–253: 8 MIME rows, Phase 3 banner |
| 12 | §4 type anti-patterns ≥2 rows per type; link §8 not duplicate (TYPE-05, D-11) | ✓ VERIFIED | §4 L198–212: PDF 3, Word 3, Excel 3, TXT 2, Markdown 3; remediation links use `#ops-matrix` |
| 13 | Bidirectional cross-ref: INGEST-PIPELINE §7.5/§8 → FILE-TYPE-PROCESSING §2/§4 | ✓ VERIFIED | PIPE L51, L470, L472, L488, L490 use `#ops-matrix` / `#ops-anti-patterns`; zero `FILE-TYPE#ops-guide` links |
| 14 | Appendix C field paths for dev TOC (D-13) | ✓ VERIFIED | L280 intro「主矩阵相关字段子集」; 4 `[^reindex-nc-*]` footnotes for non-REINDEX_FIELDS fields; `minParagraphLength` cites `application.yml` L158 |
| 15 | §9 acceptance checklist; STATE.md Phase 2 complete | ✓ VERIFIED | §9 L321–337 (7 steps); doc 416 lines (≥350) |

**Score:** 15/15 truths verified

### Gap Closure Verification (02-04 / 02-05 / 02-06)

| Prior Gap | Plan | Re-check | Result |
|-----------|------|----------|--------|
| CR-01 §2 table corruption | 02-04 | Exactly one `\| 类型 × 规则项 \|` header; Excel L105 follows Word L104 with no `### 开发参考` between; 38 matrix rows all 4-column | **CLOSED** |
| CR-02 duplicate anchors / PIPE backlinks | 02-05 | `{#ops-guide}` ×1, `{#dev-reference}` ×1 (§1 only); unique section anchors assigned; PIPE has 0 `#ops-guide` FILE-TYPE links | **CLOSED** |
| WR-01/02 Appendix C REINDEX_FIELDS scope | 02-06 | Intro scoped as subset; 4 footnotes; `minParagraphLength` → `application.yml` L158 (confirmed in live yml) | **CLOSED** |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `.planning/docs/FILE-TYPE-PROCESSING.md` | Phase 2 primary deliverable | ✓ VERIFIED | 416 lines; §2 single matrix; unique anchors; Appendix C scoped |
| `.planning/docs/INGEST-PIPELINE.md` | Back-links to Phase 2 | ✓ VERIFIED | 6 links to `#ops-matrix` / `#ops-anti-patterns`; no stale `#ops-guide` targets |
| `.planning/STATE.md` | Phase 2 marked complete | ✓ VERIFIED | Regression: unchanged from initial verification |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|-------|
| FILE-TYPE-PROCESSING §1 | INGEST-PIPELINE §5/§7/§8 | upstream cross-ref | ✓ WIRED | L43–45 |
| FILE-TYPE-PROCESSING §3 | application.yml | §3 系统默认 column | ✓ WIRED | chunkSize 600, chunkOverlap 100 match yml L154–155 |
| FILE-TYPE-PROCESSING §3 | libraryDefaults.js | §3 向导默认 column | ✓ WIRED | chunkSize 500, chunkOverlap 120 match L40–41 |
| FILE-TYPE-PROCESSING §2 | DocumentParseService.java | PDF/Word footnotes | ✓ WIRED | Dev-reference blocks L128–147 |
| FILE-TYPE-PROCESSING §4 | INGEST-PIPELINE §8 | 反模式链回 | ✓ WIRED | L195; remediation links `#ops-matrix` |
| INGEST-PIPELINE §7.5/§8 | FILE-TYPE-PROCESSING §2/§4 | Phase 2 back-link | ✓ WIRED | `#ops-matrix` / `#ops-anti-patterns` at L51, L470, L488, L490 |
| FILE-TYPE-PROCESSING §4 | §2 matrix rows | remediation links | ✓ WIRED | 14 rows link `#ops-matrix` (L199–212) |
| FILE-TYPE-PROCESSING Appendix C | libraryConfig.js REINDEX_FIELDS | scope + footnotes | ✓ WIRED | Non-Set fields footnoted; `minParagraphLength` in Set L42 |

### Data-Flow Trace (Level 4)

Doc-only phase — no runtime UI/API data flow. §3 default values traced to source files:

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|-------------------|--------|
| §3 defaults table | chunkSize / chunkOverlap | application.yml, libraryDefaults.js | 600/100 vs 500/120 | ✓ FLOWING |
| Appendix A | MIME presets | VectorLibraryConfigFactory FILE_TYPE_MIMES | 8 MIME rows | ✓ FLOWING |
| Appendix C | minParagraphLength | application.yml L158 | `min-paragraph-length: 30` | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| §2 single-table header | `rg '\| 类型 × 规则项 \|' FILE-TYPE-PROCESSING.md` | 1 match (L85) | ✓ PASS |
| §2 matrix row count | `rg '^\| \*\*' FILE-TYPE-PROCESSING.md` in L87–124 range | 38 rows, all 4 pipe-delimited columns | ✓ PASS |
| Anchor uniqueness | `rg '{#ops-guide}'` in FILE-TYPE + PIPE FILE-TYPE links | ops-guide ×1 per doc §1; 0 PIPE `#ops-guide` FILE-TYPE links | ✓ PASS |
| PIPE backlinks | `rg 'FILE-TYPE-PROCESSING.md#' INGEST-PIPELINE.md` | 6 links: all `#ops-matrix` or `#ops-anti-patterns` | ✓ PASS |
| Appendix C footnotes | `rg '\[\^reindex-nc-' FILE-TYPE-PROCESSING.md` | 4 markers + 4 definitions (L286–300) | ✓ PASS |
| Appendix C scope header | `rg '主矩阵相关字段子集' FILE-TYPE-PROCESSING.md` | L280 | ✓ PASS |
| ROADMAP three anchors | `rg '周报\|扫描\|制度' FILE-TYPE-PROCESSING.md` | 推荐/禁止 at L88, L101, L111 | ✓ PASS |
| TYPE-01–05 Covered | `rg 'TYPE-0[1-5].*Covered' FILE-TYPE-PROCESSING.md` | 10 matches | ✓ PASS |
| minParagraphLength source | Read `application.yml` L158 | `min-paragraph-length: 30` | ✓ PASS |

### Probe Execution

Step 7c: SKIPPED — no probe scripts declared for Phase 2 documentation deliverable.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| TYPE-01 | 02-01–02-06 | PDF OCR, tables, chunking, scan risks | ✓ SATISFIED | §2 L87–96; §4 L199–201 |
| TYPE-02 | 02-01–02-06 | Word structured, heading-level | ✓ SATISFIED | §2 L97–104; §4 L202–204 |
| TYPE-03 | 02-01–02-06 | Excel text-only, paragraph-first, header filter | ✓ SATISFIED | §2 L105–112; appendix B |
| TYPE-04 | 02-01–02-06 | TXT/MD encoding, paragraph chunking | ✓ SATISFIED | §2 L113–124 |
| TYPE-05 | 02-03–02-06 | Type-specific misconfig gallery | ✓ SATISFIED | §4 L198–212 (14 rows) |

No orphaned Phase 2 requirements.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| FILE-TYPE-PROCESSING.md | 234–235 | Appendix A Word all `heading-level` | ℹ️ Info | Inconsistent with §2 L101 short-doc `paragraph-first` — pre-existing, not a phase blocker |
| FILE-TYPE-PROCESSING.md | 120 | Missing `text/x-web-markdown` MIME in matrix row | ℹ️ Info | `application.yml` L92 includes it; appendix A covers primary MIMEs |

No blockers. Prior CR-01/CR-02 debt markers resolved.

### Human Verification Required

None — all prior structural gaps are programmatically verified closed. Optional: rendered-preview spot-check that §2 displays as one table and TOC jumps land on §2 vs §4.

### Gaps Summary

All three prior blockers from initial verification (CR-01 matrix structure, CR-02 anchor/backlink navigation, WR-01/02 Appendix C scope) are **closed** with codebase evidence. Phase 2 documentation deliverable meets ROADMAP goal and D-01–D-14 locked decisions. Ready to proceed to Phase 3.

---

_Verified: 2026-06-10T20:00:00Z_  
_Verifier: Claude (gsd-verifier)_
