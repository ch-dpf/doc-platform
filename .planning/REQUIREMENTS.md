# Requirements: 入库质量规范里程碑

**Defined:** 2026-06-10
**Core Value:** 预览所见分块与入库结果一致，且分块质量足以支撑检索与问答

## v1 Requirements

### Pipeline Documentation（流程梳理）

- [x] **PIPE-01**: 文档描述建库流程（向导字段 → `config_json` → `LibraryConfigResolver` 生效路径）
- [x] **PIPE-02**: 文档描述单文档入库全流程（上传 → 解析 → 清洗 → 分块 → `IndexingChunkFilter` → 嵌入 → `document_chunk`）
- [x] **PIPE-03**: 文档标注各阶段关键类与 API（含前端入口与后端服务对照）

### File-Type Matrix（类型设定对照）

- [ ] **TYPE-01**: PDF 类型说明：OCR 开关、表格提取、推荐分块策略与典型风险（扫描件、纯图 PDF）
- [ ] **TYPE-02**: Word 类型说明：`structured` 表格提取、标题分块适用场景
- [ ] **TYPE-03**: Excel 类型说明：`text-only`、续行合并、`paragraph-first`、表头块过滤、不适用 semantic 分块
- [ ] **TYPE-04**: TXT/Markdown 类型说明：编码检测、段落分块参数建议
- [ ] **TYPE-05**: 各类型「错误设定示例」与「预期分块质量问题」对照（运营可读）

### Library Presets（库预设）

- [ ] **PRESET-01**: 定义至少 4 种库预设：周报 Excel 库、制度/长文库、报销扫描库、通用混合库
- [ ] **PRESET-02**: 每预设包含完整 `parsing` + `cleaning` + `chunking` 子配置及一句话适用说明
- [ ] **PRESET-03**: 建库向导可选择预设并填充表单（`CreateLibraryWizard.vue`）
- [ ] **PRESET-04**: 编辑库设置时可查看当前预设来源或「自定义」状态

### Preview–Index Parity（预览与入库一致）

- [ ] **PARITY-01**: 采集分块预览与 `IndexingService` 使用相同分块参数与 `IndexingChunkFilter` 规则
- [ ] **PARITY-02**: 预览展示的 `rawTotalChunks` / `filteredOutCount` / 最终块数与入库后 chunk 数一致（同文本输入）
- [ ] **PARITY-03**: 库配置变更后预览即时反映（含 `tableExtraction` diff 保存）
- [ ] **PARITY-04**: 后端单测覆盖至少 1 个周报样本的预览 vs 索引块数一致性

### Config UX（配置可预期）

- [ ] **CFG-01**: `diffLibraryConfig` 正确比较 `parsing.*` / `chunking.*` / `cleaning.*` 嵌套字段
- [ ] **CFG-02**: 向导/编辑页对「表格提取」「OCR」等选项展示简短影响说明（非仅字段名）

## v2 Requirements

### Quality Gates

- **GATE-01**: 入库前自动检测「仅表头块占比过高」并警告
- **GATE-02**: 入库报告展示每文档 chunk 数、过滤数、平均块长

### Structured Ingest

- **STRUCT-01**: Excel 行级结构化入库（另里程碑）

## Out of Scope

| Feature | Reason |
|---------|--------|
| RAG 问答规则扩展 | 本里程碑解决入库源头质量，非生成层 |
| 新向量模型/重排模型选型 | 与分块质量无直接关系 |
| 完整 POI Excel 解析 | 推迟到结构化里程碑 |
| 前端自动化测试框架 | v1 用手工 + 后端单测验收 |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| PIPE-01 | Phase 1 | Complete |
| PIPE-02 | Phase 1 | Complete |
| PIPE-03 | Phase 1 | Complete |
| TYPE-01 | Phase 2 | Pending |
| TYPE-02 | Phase 2 | Pending |
| TYPE-03 | Phase 2 | Pending |
| TYPE-04 | Phase 2 | Pending |
| TYPE-05 | Phase 2 | Pending |
| PRESET-01 | Phase 3 | Pending |
| PRESET-02 | Phase 3 | Pending |
| PRESET-03 | Phase 3 | Pending |
| PRESET-04 | Phase 3 | Pending |
| PARITY-01 | Phase 4 | Pending |
| PARITY-02 | Phase 4 | Pending |
| PARITY-03 | Phase 4 | Pending |
| PARITY-04 | Phase 4 | Pending |
| CFG-01 | Phase 5 | Pending |
| CFG-02 | Phase 5 | Pending |

**Coverage:**

- v1 requirements: 17 total
- Mapped to phases: 17
- Unmapped: 0 ✓

---
*Requirements defined: 2026-06-10*
*Last updated: 2026-06-10 after initial definition*
