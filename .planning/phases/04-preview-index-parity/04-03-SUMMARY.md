---
phase: 04-preview-index-parity
plan: 03
subsystem: ui
tags: [vue, ingest, chunk-preview, parity]

requires:
  - phase: 04-preview-index-parity
    provides: backend libraryId-resolved chunk preview from plan 04-02
provides:
  - IngestView 移除 overrideChunk，预览绑定 libraryChunkParams
  - CreateLibraryWizard 使用 libraryChunkParams 替代临时缩放
  - 库设置保存后 IngestView 热刷新 libraryConfig
affects: [05-config-ux]

tech-stack:
  added: []
  patterns: [libraryChunkParams as single preview source, config hot-reload on saved]

key-files:
  created: []
  modified:
    - frontend/knowbase-ui/src/views/IngestView.vue
    - frontend/knowbase-ui/src/components/CreateLibraryWizard.vue
    - frontend/knowbase-ui/src/utils/chunkPreviewSample.js

key-decisions:
  - "libraryChunkParams 默认值对齐 libraryDefaults（chunkSize=500, overlap=120）"
  - "resolvePreviewChunkParams 标记 @deprecated 保留向后兼容"

requirements-completed: [PARITY-01, PARITY-03]

duration: 20min
completed: 2026-06-10
---

# Phase 04 Plan 03：前端预览一致性 Summary

**移除采集页预览专用 chunk 覆盖，向导与入库页统一使用 libraryChunkParams，库设置保存后自动刷新配置**

## 成果

- IngestView：删除 overrideChunkEnabled/overrideChunkSize 及「仅本次预览与入库」文案
- buildChunkPreviewBody 始终发送 libraryId + libraryChunkParams 完整参数
- onLibrarySettingsSaved 立即应用 saved.config 并重新拉取库详情
- CreateLibraryWizard：用 libraryChunkParams 替代 resolvePreviewChunkParams，展示策略提示
- chunkPreviewSample.js：libraryChunkParams 默认值与 libraryDefaults 对齐

## Task Commits

1. **Task 1: IngestView — remove overrideChunk + config reload** - `b8e8b3e`
2. **Task 2: CreateLibraryWizard — libraryChunkParams** - `df1bc66`

## 验证

- grep：IngestView.vue 无 overrideChunk
- grep：CreateLibraryWizard.vue 无 resolvePreviewChunkParams
- `npm run build` 成功

## Deviations from Plan

None - plan executed exactly as written.

## Self-Check: PASSED

- FOUND: frontend/knowbase-ui/src/views/IngestView.vue
- FOUND: frontend/knowbase-ui/src/components/CreateLibraryWizard.vue
- FOUND: commit b8e8b3e
- FOUND: commit df1bc66

---
*Phase: 04-preview-index-parity*
*Completed: 2026-06-10*
