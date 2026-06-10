---
phase: 04-preview-index-parity
plan: 01
subsystem: api
tags: [java, spring, chunking, parity, pipeline]

requires:
  - phase: 03-library-presets
    provides: LibraryConfigResolver chunking/cleaning/normalization resolution
provides:
  - LibraryChunkPipeline shared normalize→clean→chunk→filter pipeline
  - ChunkPipelineResult with rawTotalChunks and filteredOutCount
  - IndexingService index path without double-clean
affects: [04-preview-index-parity, ingest-ui]

tech-stack:
  added: []
  patterns: [shared chunk pipeline, chunkIndexedText for pre-cleaned parsed.txt]

key-files:
  created:
    - knowbase-service/src/main/java/com/knowbase/vector/chunk/LibraryChunkPipeline.java
    - knowbase-service/src/main/java/com/knowbase/vector/chunk/ChunkPipelineResult.java
  modified:
    - knowbase-service/src/main/java/com/knowbase/vector/service/IndexingService.java
    - knowbase-service/src/main/java/com/knowbase/vector/service/ChunkPreviewService.java
    - knowbase-service/src/test/java/com/knowbase/vector/service/ChunkPreviewServiceTest.java

key-decisions:
  - "chunk() 走完整 normalize+clean+chunk+filter；chunkIndexedText() 跳过 normalize/clean（parsed.txt 已处理）"
  - "无 libraryId 的预览仍走 chunkWithRequestConfig 保留向导建库前兼容"

requirements-completed: [PARITY-01, PARITY-02]

duration: 25min
completed: 2026-06-10
---

# Phase 04 Plan 01：共享分块流水线 Summary

**提取 LibraryChunkPipeline，消除 IndexingService 对 parsed.txt 二次清洗导致的预览/入库分块漂移**

## 成果

- 新增 `ChunkPipelineResult` 记录 chunks、rawTotalChunks、filteredOutCount、processedText
- 新增 `LibraryChunkPipeline`：`chunk()` 全链路预览；`chunkIndexedText()` 仅 chunk+filter
- `IndexingService.index()` 不再调用 `documentCleaningService.apply`
- `ChunkPreviewService` 委托共享流水线，杜鹏飞周报 fixture 测试通过（raw=4, filtered=1, total=3）

## Task Commits

1. **Task 1: Define LibraryChunkPipeline contract** - `7561964`
2. **Task 2: Wire services** - `7535108`

## 验证

- `mvn -Dtest=ChunkPreviewServiceTest,IndexingChunkFilterTest test` 通过
- grep 确认 IndexingService 无 documentCleaningService 调用

## Deviations from Plan

None - plan executed exactly as written.

## Self-Check: PASSED

- FOUND: knowbase-service/src/main/java/com/knowbase/vector/chunk/LibraryChunkPipeline.java
- FOUND: knowbase-service/src/main/java/com/knowbase/vector/chunk/ChunkPipelineResult.java
- FOUND: commit 7561964
- FOUND: commit 7535108

---
*Phase: 04-preview-index-parity*
*Completed: 2026-06-10*
