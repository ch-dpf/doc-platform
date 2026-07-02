# 项目实现进度（文档 ↔ 代码对照）

本文将 **当前代码实现** 与 **设计/二期文档** 对齐，便于判断「已交付 / 部分 / 规划中」。更新于 2026-07。

图例：**✅ 已交付** · **🟡 部分** · **⬜ 规划中**

---

## 一期主链路（RAG 纵向切片）

| 能力 | 状态 | 代码 / 入口 |
|------|------|-------------|
| 知识库 CRUD + 库类型预设 | ✅ | `LibraryCatalogController`、`BuiltinPresetCatalog` |
| 文档上传 / 批量入库 | ✅ | `IngestionRunController`、`DocumentIngestWizard.vue` |
| 分阶段 prepare（解析/清洗/切块预览） | ✅ | `PrepareIngestionUseCase`、`DocumentIngestWizard.vue` |
| Parser Profile 自动路由 | ✅ | `DocumentSourceLoader`、`IngestionProductCatalog` |
| 14 内置 + 3 外接解析器 | ✅ | `knowbase-ingestion/*Parser` |
| 文本清洗 + 结构块规范化 | ✅ | `DocumentTextNormalizer` |
| Token 驱动切分 + 字符兜底 | ✅ | `TokenBasedDocumentChunker` |
| Embedding + pgvector 写入 | ✅ | `DefaultIngestionPipeline`、`knowbase-persistence` |
| 索引代次（active / publish） | ✅ | `IndexGenerationService`、settings API |
| 智能体 + 多库问答 | ✅ | `QueryRunController`、`QaPage.vue` |
| 智能体级检索测试 | ✅ | `AgentController` retrieval-tests |
| Pipeline Trace / 观测 | ✅ | `ObservabilityController`、`PipelineTraceTimeline.vue` |

---

## 已超前于二期文档 §4「缺项」的能力

以下在 `PHASE2_INGESTION_PLAN.md` §4 中仍标为不足，**代码已部分落地**（文档待同步）：

| 能力 | 状态 | 说明 |
|------|------|------|
| 库级召回测试页 | ✅ | `LibraryRetrievalTestPage.vue`、`/libraries/:id/retrieval-test` |
| 黄金集 + Recall@K | 🟡 | 后端与 UI 已有；发布门禁默认关闭 `promote-eval-gate-enabled: false` |
| PaddleOCR-VL / vLLM 扫描路由 | 🟡 | `PdfLayoutParser`、`vision-document` 配置；需自部署服务 |
| 外接 Docling/Unstructured | 🟡 | `ExternalDocumentParser` + fallback；协议未完全版本化 |
| 单文档 / 批量 reindex | ✅ | `reindex`、`reindex-failed`、`reindex-by-profile` API + settings UI |
| PDF citation bbox 预览 | ✅ | `PdfPreviewPanel.vue`、QA 引用跳转 |
| Excel 自适应表解析 | ✅ | `StructuredTableDocumentParser`、`table-deep` |
| 离线 ingestion eval | ✅ | `run-ingestion-eval.ps1`、`sample-documents` 回归集 |
| 文档 upsert | 🟡 | `document-upsert-enabled`；完整生命周期同步仍规划 |
| 结构化入库日志 | ✅ | `DocumentPreparationPipeline`、`DefaultIngestionPipeline` 中文 SLF4J |

---

## 二期规划（仍待加强）

| 二期章节 | 状态 | 差距摘要 |
|----------|------|----------|
| 3.1 PDF 深度解析 | 🟡 | 启发式 + 可选 reading-order；复杂/无边框/跨页表格弱 |
| 3.2 OCR 深度解析 | 🟡 | Tesseract + VLM 路由；专用 reading-order 服务非默认 |
| 3.3 表格语义增强 | 🟡 | Excel 较强；PDF cell 级 bbox 不足 |
| 3.4 语义切分评测 | 🟡 | 离线回归有；semantic chunk 未产品化 |
| 3.5 外接解析器协议 | 🟡 | adapter 有；Schema 版本化、完整 table 映射缺 |
| 3.6 Profile 治理 | 🟡 | settings 页有；全版本 diff/回滚 UI 不完整 |
| 3.7 召回测试 | 🟡 | 单库沙盒有；批量评测报告与发布门禁未闭环 |
| 3.8 文档生命周期 / 同步 | ⬜ | 删除/reindex 有；目录/对象存储同步任务无 |
| 3.9 入库任务运维 | 🟡 | 单任务查询有；列表/取消/中间产物缓存缺 |
| 3.10 可观测聚合 | 🟡 | span 有；阶段瀑布图与指标聚合弱 |

---

## 前端控制台路由（与 README 一致）

| 路由 | 状态 |
|------|------|
| `/libraries/:id/retrieval-test` | ✅ |
| `/libraries/:id/settings` | ✅ Profile + reindex |
| `/libraries/:id/documents/:docId` | ✅ 原文/chunk/trace |
| Chat 多轮会话 UI | ⬜ 后端 `/chat/sessions` 有，控制台未接 |

---

## 本地环境默认值（2026-07 调整后）

| 项 | 默认 | 说明 |
|----|------|------|
| 后端端口 | `8080` | `application.yml`；IDEA 勿混用 8088 |
| PostgreSQL | `localhost:5433` | Docker `knowbase-postgres` 或本机 PG |
| 对象存储 | `local` | `./data/knowbase-storage`；MinIO 见 `application-dev.yml` 注释 |
| Ollama | 可选 | 未启动时 layout/reading-order 回退启发式 |
| PaddleOCR-VL | 默认关闭 | `dev` profile 或手动开启 |
| 前端代理 | `127.0.0.1:8080` | `vite.config.js` / `.env.development` |

---

## 维护说明

- 模块边界变更 → 更新 [MODULES.md](MODULES.md)
- 二期条目完成 → 修改本文 + `PHASE2_INGESTION_PLAN.md` §4 对应小节
- 新增 REST/UI → 更新 [README.md](../README.md) 路由表与 [API.md](API.md)
