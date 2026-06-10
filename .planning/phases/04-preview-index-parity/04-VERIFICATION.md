---
phase: 04-preview-index-parity
verified: 2026-06-10T07:15:00Z
status: passed
score: 9/9 must-haves verified
overrides_applied: 0
---

# Phase 4: 预览与入库一致性 Verification Report

**Phase Goal:** `ChunkPreviewService` 与 `IndexingService` 同规则，块数与内容对齐。
**Verified:** 2026-06-10T07:15:00Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | ------- | ---------- | -------------- |
| 1 | IndexingService 与 ChunkPreviewService 对相同文本与库配置产出相同分块列表 | ✓ VERIFIED | `LibraryChunkPipeline.chunk()` 与 `chunkIndexedText(processedText)` 在 `LibraryChunkPipelineParityTest.previewMatchesIndexPathForWeeklyReportSample` 中断言 chunks/rawTotalChunks/filteredOutCount 完全一致 |
| 2 | IndexingService 不对 parsed.txt 二次清洗 | ✓ VERIFIED | `IndexingService.index()` 调用 `libraryChunkPipeline.chunkIndexedText()`；grep 无 `documentCleaningService`/`ParsedTextNormalizer` 引用 |
| 3 | 两条路径均在 `ChunkingService.chunk` 后应用 `IndexingChunkFilter.removeHeaderOnlyChunks` | ✓ VERIFIED | `LibraryChunkPipeline.chunkAndFilter()` 统一调用 filter |
| 4 | 杜鹏飞周报样本自动化 parity 测试（raw=4, filtered=1, total=3） | ✓ VERIFIED | `LibraryChunkPipelineParityTest` + `ChunkPreviewServiceTest` 通过；`mvn -Dtest=LibraryChunkPipelineParityTest,ChunkPreviewServiceTest,IndexingChunkFilterTest test` BUILD SUCCESS |
| 5 | libraryId 存在时预览忽略客户端 chunkSize 覆盖 | ✓ VERIFIED | `ChunkPreviewService.preview()` 在 `libraryId != null` 时仅调用 `libraryChunkPipeline.chunk()`；`clientOverrideIgnoredWhenLibraryIdSet` 用 chunkSize=50 仍返回 total=3 |
| 6 | IngestView 预览仅使用 libraryChunkParams，无 overrideChunk | ✓ VERIFIED | grep 无 `overrideChunk`/`仅本次预览`；`buildChunkPreviewBody` 始终 `libraryChunkParams(cfg)` + `libraryId` |
| 7 | CreateLibraryWizard 使用 libraryChunkParams 替代临时缩放 | ✓ VERIFIED | grep 无 `resolvePreviewChunkParams`；预览体使用 `libraryChunkParams(form.config)` + `CHUNK_STRATEGY_PREVIEW_HINTS` |
| 8 | 库设置保存后 IngestView 热刷新 libraryConfig | ✓ VERIFIED | `EditLibrarySettingsDrawer` emit `saved`；`onLibrarySettingsSaved` 更新 config 并 `loadCurrentLibrary()`；下次 `buildChunkPreviewBody` 读取新 config |
| 9 | 「预览 8 块入库 3 块」类漂移不可复现（同配置同文本） | ✓ VERIFIED | 共享流水线 + 去重清洗 + libraryId 权威解析 + 前端移除预览专用覆盖 + 回归单测锚定杜鹏飞 fixture |

**Score:** 9/9 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | ----------- | ------ | ------- |
| `knowbase-service/.../LibraryChunkPipeline.java` | 共享 normalize→clean→chunk→filter 流水线 | ✓ VERIFIED | `chunk()` / `chunkIndexedText()` / `chunkWithRequestConfig()` 三入口；`chunkAndFilter` 统一 filter |
| `knowbase-service/.../ChunkPipelineResult.java` | rawTotalChunks, filteredOutCount, chunks, processedText | ✓ VERIFIED | record 四字段齐全 |
| `knowbase-service/.../IndexingService.java` | index 路径无二次清洗 | ✓ VERIFIED | 仅 `chunkIndexedText` |
| `knowbase-service/.../ChunkPreviewService.java` | 委托共享流水线 | ✓ VERIFIED | libraryId 走 `chunk()`，否则 `chunkWithRequestConfig()` |
| `knowbase-service/.../LibraryChunkPipelineParityTest.java` | PARITY-04 回归测试 | ✓ VERIFIED | 含 `previewMatchesIndexPathForWeeklyReportSample`、`clientOverrideIgnoredWhenLibraryIdSet` |
| `frontend/.../IngestView.vue` | 无 override；config 热刷新 | ✓ VERIFIED | `buildChunkPreviewBody` + `onLibrarySettingsSaved` |
| `frontend/.../CreateLibraryWizard.vue` | libraryChunkParams 预览 | ✓ VERIFIED | 无 `resolvePreviewChunkParams` |
| `frontend/.../chunkPreviewSample.js` | 默认值对齐 libraryDefaults | ✓ VERIFIED | chunkSize=500, overlap=120；`resolvePreviewChunkParams` @deprecated |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | --- | --- | ------ | ------- |
| IndexingService | LibraryChunkPipeline | chunkIndexedText | ✓ WIRED | line 146 |
| ChunkPreviewService | LibraryChunkPipeline | chunk / chunkWithRequestConfig | ✓ WIRED | lines 28-37 |
| LibraryChunkPipeline | IndexingChunkFilter | removeHeaderOnlyChunks | ✓ WIRED | chunkAndFilter |
| IngestView | chunk-preview API | buildChunkPreviewBody | ✓ WIRED | libraryId + libraryChunkParams |
| EditLibrarySettingsDrawer | IngestView | @saved → onLibrarySettingsSaved | ✓ WIRED | reload library config |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| -------- | ------------- | ------ | ------------------ | ------ |
| ChunkPreviewService | result.chunks | LibraryChunkPipeline.chunk | 杜鹏飞 fixture 真实分块 | ✓ FLOWING |
| IndexingService | pipeline.chunks | chunkIndexedText(parsed.txt) | 与 preview processedText 路径一致 | ✓ FLOWING |
| IngestView buildChunkPreviewBody | sizing | libraryChunkParams(libraryConfig) | 来自 getVectorLibrary / saved config | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| 杜鹏飞 parity 单测 | `mvn -Dtest=LibraryChunkPipelineParityTest,ChunkPreviewServiceTest,IndexingChunkFilterTest test` | 5 tests, 0 failures, BUILD SUCCESS | ✓ PASS |
| IngestView 无 overrideChunk | `rg overrideChunk IngestView.vue` | 0 matches | ✓ PASS |
| Wizard 无 resolvePreviewChunkParams | `rg resolvePreviewChunkParams CreateLibraryWizard.vue` | 0 matches | ✓ PASS |

### Probe Execution

Step 7c: SKIPPED — no probe scripts declared for this phase.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ---------- | ----------- | ------ | -------- |
| PARITY-01 | 04-01, 04-03 | 预览与 IndexingService 相同分块参数与 filter 规则 | ✓ SATISFIED | LibraryChunkPipeline + 前端 libraryChunkParams |
| PARITY-02 | 04-01, 04-02 | rawTotalChunks/filteredOutCount/最终块数与入库一致 | ✓ SATISFIED | chunk vs chunkIndexedText parity 测试；IndexingService 共享 filter |
| PARITY-03 | 04-03 | 库配置变更后预览即时反映 | ✓ SATISFIED | onLibrarySettingsSaved + loadCurrentLibrary |
| PARITY-04 | 04-02 | 周报样本预览 vs 索引块数一致性单测 | ✓ SATISFIED | LibraryChunkPipelineParityTest |

**Note:** `REQUIREMENTS.md` traceability table 中 PARITY-02/PARITY-03 仍显示 Pending（phase.complete 未同步 body checkbox），但代码与测试证据已满足 — 属文档同步问题，非实现缺口。

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| — | — | — | — | 阶段修改文件无 TBD/FIXME/XXX/stub 模式 |

### Human Verification Required

无 — 所有 must-have 均可通过代码与单测验证。

### Gaps Summary

无缺口。Phase 4 目标已达成。

---

_Verified: 2026-06-10T07:15:00Z_
_Verifier: Claude (gsd-verifier)_
