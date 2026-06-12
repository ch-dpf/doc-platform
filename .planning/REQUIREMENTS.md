# Requirements

**Defined:** 2026-06-10  
**Core Value:** 预览所见分块与入库结果一致，且分块质量足以支撑检索与 RAG 可答

> **战略更新（2026-06-10）：** v2 起 **Greenfield 实现、无数据兼容**；v1 需求为规范基线；v2/v3 见下方草案。

## v1 Requirements（规范里程碑 — Complete）

### Pipeline Documentation（流程梳理）

- [x] **PIPE-01**: 文档描述建库流程（向导字段 → `config_json` → `LibraryConfigResolver` 生效路径）
- [x] **PIPE-02**: 文档描述单文档入库全流程（上传 → 解析 → 清洗 → 分块 → `IndexingChunkFilter` → 嵌入 → `document_chunk`）
- [x] **PIPE-03**: 文档标注各阶段关键类与 API（含前端入口与后端服务对照）

### File-Type Matrix（类型设定对照）

- [x] **TYPE-01**: PDF 类型说明：OCR 开关、表格提取、推荐分块策略与典型风险（扫描件、纯图 PDF）
- [x] **TYPE-02**: Word 类型说明：`structured` 表格提取、标题分块适用场景
- [x] **TYPE-03**: Excel 类型说明：`text-only`、续行合并、`paragraph-first`、表头块过滤、不适用 semantic 分块
- [x] **TYPE-04**: TXT/Markdown 类型说明：编码检测、段落分块参数建议
- [x] **TYPE-05**: 各类型「错误设定示例」与「预期分块质量问题」对照（运营可读）

### Library Presets（库预设）— **Superseded 2026-06-10**

> 库物种 preset（垂直/通用、场景模板）已废止。建库统一 `libraryDefaults.js`；MIME 默认由 `parsing.mimeAwareDefaults` 控制。下列 PRESET 需求仅作 Phase 3 历史记录。

- [x] ~~**PRESET-01**~~: ~~4 种场景库预设~~ → 已删除 `libraryPresets.js`
- [x] ~~**PRESET-02**~~: ~~预设子配置~~ → 并入 `libraryDefaults` + 向导 Step 3
- [x] ~~**PRESET-03**~~: ~~向导选预设~~ → 已移除 Step 1 库类型卡片
- [x] ~~**PRESET-04**~~: ~~编辑页 preset 标签~~ → 已移除；无 `libraryPresetId`

### Preview–Index Parity（预览与入库一致）

- [x] **PARITY-01**: 采集分块预览与 `IndexingService` 使用相同分块参数与 `IndexingChunkFilter` 规则
- [x] **PARITY-02**: 预览展示的 `rawTotalChunks` / `filteredOutCount` / 最终块数与入库后 chunk 数一致（同文本输入）
- [x] **PARITY-03**: 库配置变更后预览即时反映（含 `tableExtraction` diff 保存）
- [x] **PARITY-04**: 后端单测覆盖至少 1 个周报样本的预览 vs 索引块数一致性

### Config UX（配置可预期）

- [x] **CFG-01**: `diffLibraryConfig` 正确比较 `parsing.*` / `chunking.*` / `cleaning.*` 嵌套字段
- [x] **CFG-02**: 向导/编辑页对「表格提取」「OCR」等选项展示简短影响说明（非仅字段名）

## v2 Requirements（目标态建仓入库 — Draft）

> 详见 `.planning/MILESTONE-v2-DRAFT.md`；正式化 via `/gsd-new-milestone`

### Greenfield Platform

- **GF-01**: 按目标态重建 PG schema，**不要求**迁移旧数据
- **GF-02**: 后端/前端代码可按目标态**整体重写**，技术栈不变

### Target-State Ingest（RAG 上游）

- **ING-01**: 建库向导 + `libraryDefaults` + §6 语义边界指引（无库物种二分）
- **ING-02**: 三层配置：系统默认 / 库默认 / 采集 ingest profile
- **ING-03**: 入库管道：解析 → 清洗 → 分块 → filter → 嵌入 → `document_chunk`
- **ING-04**: 预览 = 入库（单一 chunk 管道契约）
- **ING-05**: 按 TYPE 矩阵运行时生效（PDF/Word/Excel/TXT/MD）
- **ING-06**: hybrid 检索 API + chunk metadata 契约稳定（供 v3 消费）

### Quality Gates（v2 可选波次）

- **GATE-01**: 入库前「表头块占比过高」警告
- **GATE-02**: 入库报告：chunk 数、过滤数、平均块长

### Deferred

- **STRUCT-01**: Excel 行级结构化入库 — 另里程碑

## v3 Requirements（RAG 智能问答 — Draft）

> 详见 `.planning/MILESTONE-v3-DRAFT.md`；**依赖 v2 完成**

- **RAG-01**: 库内问答（检索 + Ollama 生成 + 引用 chunk）
- **RAG-02**: 多轮对话与会话持久化
- **RAG-03**: 引用溯源 UI（文档/块跳转）
- **RAG-04**: 检索 trace（召回块、分数、过滤原因）
- **RAG-05**: 流式 SSE 生成
- **RAG-06**: RAG 可答率评测样本集（D-16 工程化）

## Out of Scope

| Feature | Reason |
|---------|--------|
| 历史数据迁移 | 用户明确可全量清除 |
| 旧代码 API 兼容 | Greenfield 重写 |
| 新向量/LLM 平台选型 | 技术栈锁定 |
| `document_record` 双轨 | 另立里程碑 |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| PIPE-01 | Phase 1 | Complete |
| PIPE-02 | Phase 1 | Complete |
| PIPE-03 | Phase 1 | Complete |
| TYPE-01 | Phase 2 | Complete |
| TYPE-02 | Phase 2 | Complete |
| TYPE-03 | Phase 2 | Complete |
| TYPE-04 | Phase 2 | Complete |
| TYPE-05 | Phase 2 | Complete |
| PRESET-01 | Phase 3 | Complete |
| PRESET-02 | Phase 3 | Complete |
| PRESET-03 | Phase 3 | Complete |
| PRESET-04 | Phase 3 | Complete |
| PARITY-01 | Phase 4 | Complete |
| PARITY-02 | Phase 4 | Complete |
| PARITY-03 | Phase 4 | Complete |
| PARITY-04 | Phase 4 | Complete |
| CFG-01 | Phase 5 | Complete |
| CFG-02 | Phase 5 | Complete |

**Coverage:**

- v1 requirements: 17 total
- Mapped to phases: 17
- Unmapped: 0 ✓

---
*Requirements defined: 2026-06-10*
*Last updated: 2026-06-10 — Greenfield strategy; v2 ingest + v3 RAG requirement drafts*
