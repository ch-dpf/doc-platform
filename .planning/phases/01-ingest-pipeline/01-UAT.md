---
status: testing
phase: 01-ingest-pipeline
source: 01-VERIFICATION.md
started: 2026-06-08T12:00:00Z
updated: 2026-06-10T15:30:00Z
---

## Current Test

number: 1
name: §9 新人五步追溯
expected: 按 §9 验收清单五步追溯，全程不打开 Java/Vue 源码。每步仅通过 INGEST-PIPELINE.md 内章节锚点与表格即可完成：建库字段→config_json→上传 API→九阶段管道→document_chunk 写入；第 5 步明确预览块数≠入库块数。
awaiting: user response

## Tests

### 1. §9 新人五步追溯
expected: 五步追溯无歧义，不依赖源码即可理解建库到 document_chunk 全路径
result: [pending]

### 2. Mermaid 三图渲染
expected: 在 Markdown 预览中打开 INGEST-PIPELINE.md，§2.1 flowchart、§3.1 sequenceDiagram、§6.1 flowchart 三图均正常渲染，节点文字可读
result: [pending]

## Summary

total: 2
passed: 0
issues: 0
pending: 2
skipped: 0
blocked: 0

## Gaps

[none yet]
