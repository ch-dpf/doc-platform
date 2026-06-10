---
phase: 04-preview-index-parity
plan: 02
subsystem: testing
tags: [java, tdd, parity, regression-test]

requires:
  - phase: 04-preview-index-parity
    provides: LibraryChunkPipeline from plan 04-01
provides:
  - LibraryChunkPipelineParityTest 杜鹏飞 fixture 回归测试
  - libraryId 存在时忽略客户端 chunk 覆盖
affects: [04-preview-index-parity, ingest-ui]

tech-stack:
  added: []
  patterns: [parity test chunk vs chunkIndexedText, resolver-authoritative preview]

key-files:
  created:
    - knowbase-service/src/test/java/com/knowbase/vector/chunk/LibraryChunkPipelineParityTest.java
  modified:
    - knowbase-service/src/main/java/com/knowbase/vector/dto/ChunkPreviewRequest.java

key-decisions:
  - "libraryId 非空时 ChunkPreviewService 仅使用 LibraryConfigResolver，忽略请求体分块参数"

requirements-completed: [PARITY-02, PARITY-04]

duration: 15min
completed: 2026-06-10
---

# Phase 04 Plan 02：预览/入库 parity 测试 Summary

**新增 LibraryChunkPipelineParityTest，确保杜鹏飞周报样本在 preview 与 index 路径分块一致，且 libraryId 存在时客户端 chunkSize 覆盖无效**

## 成果

- `previewMatchesIndexPathForWeeklyReportSample`：chunk() 与 chunkIndexedText(processedText) 结果一致
- `clientOverrideIgnoredWhenLibraryIdSet`：libraryId + 错误 chunkSize 仍返回 total=3
- `ChunkPreviewRequest` JavaDoc 说明 libraryId 存在时分块参数由库配置解析

## Task Commits

1. **Task 1: RED — LibraryChunkPipelineParityTest** - `1d600ac` (test)
2. **Task 2: GREEN — libraryId-resolved preview** - `01d19b2` (feat)

## TDD Gate Compliance

- test commit `1d600ac` 在 feat commit `01d19b2` 之前
- 实现已在 04-01 完成，GREEN 阶段主要为 JavaDoc 与测试锚定

## 验证

- `mvn -Dtest=LibraryChunkPipelineParityTest,ChunkPreviewServiceTest test` 通过

## Deviations from Plan

None - plan executed exactly as written.

## Self-Check: PASSED

- FOUND: knowbase-service/src/test/java/com/knowbase/vector/chunk/LibraryChunkPipelineParityTest.java
- FOUND: commit 1d600ac
- FOUND: commit 01d19b2

---
*Phase: 04-preview-index-parity*
*Completed: 2026-06-10*
