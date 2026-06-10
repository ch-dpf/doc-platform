# Phase 1: 全链路流程梳理 - Context

**Gathered:** 2026-06-10
**Status:** Ready for planning

<domain>
## Phase Boundary

交付**目标态为主**的建库与入库流程文档：明确系统级 / 库级 / 采集级规则归属，定义垂直专用库与通用混合库的选型与建库路径，描述单文档从上传到 `document_chunk` 写入的阶段与关键类，并给出分块「高可用」准则与反模式样本。

本阶段**不写代码**；实现差距在附录标注。采集级覆盖、MIME 自动默认、软锁定重索引等列为后续 phase/backlog，但须在目标态文档中完整描述。

</domain>

<decisions>
## Implementation Decisions

### 规划基线（目标态 vs 现状）

- **D-01:** 流程文档以**合理目标架构**为主叙述，不以「仅描述现有代码」为约束；每章末尾或附录用「当前差距」对照 `LibraryConfigResolver`、 `lockPipeline`、`overrideChunk` 等现状。
- **D-02:** 愿景与 v1 交付拆分——愿景包含通用库一等公民；v1 里程碑交付流程文档 + 召回层质量准则 + 反模式样本，**不**将 RAG 答对率纳入 v1 工程验收。

### 三层配置边界

- **D-03（系统级）:** 仅固定**基础设施上限**——单文件/批量大小、OCR 引擎可用性、全局 embedding 模型兜底；不承载业务型解析/分块策略。
- **D-04（库级）:** **必须统一**的规则仅限**向量 + 检索 + 治理上限**——embedding 模型/维度、hybrid/rerank、`metadataFilterFields` 白名单、容量、版本策略、支持类型上限。
- **D-05（库级默认，可采集覆盖）:** 解析、清洗、分块策略作为**库级默认值**，目标态允许采集级覆盖（OCR、chunk 参数、语义标签等）；与现状「整条管道锁在库级」的差异写入「当前差距」。
- **D-06（采集级）:** v1 **不实现**持久化 ingest profile；文档规划采集级能力（覆盖项白名单、预览=入库、写入 `doc_metadata` 或 ingest job）。`documentMetadata` 在目标态为**语义标签**（检索过滤），**不**驱动解析/分块管道（除非未来扩展 ingest profile）。
- **D-07（文档呈现）:** 使用**配置矩阵表**——行=规则项，列=系统/库默认/采集覆盖，单元格标注「必须 / 默认 / 可覆盖 / 禁止」。

### 库类型谱系与建库策略

- **D-08:** 定义**两类**一等公民库：**垂直专用库**（同质语义，可混合文件类型）、**通用混合库**（多类型多语义）。
- **D-09:** **通用混合库为一等公民**（目标态）——通过 MIME 自动默认 + 采集语义标签 + 库级检索配置，力争分块质量可支撑 RAG；v1 文档描述路径，实现分阶段落地。
- **D-10:** 垂直专用库判定用**启发式清单**——同语义 + 同问答模式 → 专用库；异质语义即使同扩展名（如周报 xlsx vs 报销 xlsx）**应拆库**。
- **D-11:** **以语义为主轴**——同质语义允许混合类型（例：周报 xlsx + 导出 pdf 同库），靠 MIME 感知默认分化处理；不以「文件扩展名」作为建库唯一依据。
- **D-12:** 建库流程文档包含**选型决策树**——业务场景 → 库类型（专用/通用）→ 预设（Phase 3）→ 高级微调。

### 入库流程与配置锁定

- **D-13:** 流程粒度 = **阶段 + 关键类**（对齐 PIPE-03），覆盖：上传 → 解析 → 清洗 → 分块 → `IndexingChunkFilter` → 嵌入 → `document_chunk`。
- **D-14:** 预览与入库一致性——文档**标注现状缺口**（`overrideChunk` 仅预览、索引走库级；`ChunkPreviewService` vs `IndexingService` 路径差异），目标态与 Phase 4（PARITY）对齐。
- **D-15:** `lockPipeline` 目标态 = **软锁定**——允许修改 parsing/cleaning/chunking/embedding 规则，但必须触发**全库重索引**任务，UI 强警告；现状硬锁定记入「当前差距」。

### 分块质量「高可用」

- **D-16:** 质量**北极星** = **RAG 可答率**（块内信息足以支撑正确回答）；v1 文档验收层 = **检索可召回**（块自洽、非纯表头、关键事实不无故跨块断裂、预览=入库目标态）。
- **D-17:** Phase 1 写**通用质量准则**；按类型的细表引用 Phase 2（`FILE-TYPE-PROCESSING.md`），不在 Phase 1 重复展开。
- **D-18:** 必须包含**反模式对照 + 真实样本**（杜鹏飞周报 xlsx、扫描 PDF OCR 关闭、表头块占比过高等）。
- **D-19:** v2 质量门禁（GATE-01/02）在文档中**列入 backlog 引用**，定义准则但不承诺 v1 实现。

### Claude's Discretion

- 决策树与配置矩阵的具体章节编号、附录命名（`INGEST-PIPELINE.md` 内嵌 vs 拆 `CONFIG-TIERS.md`）由 planner 根据可读性决定，须保证运营与开发各有一份可追踪入口。
- 「当前差距」附录的粒度：至少覆盖 `DocumentPipelineService`、`IndexingService`、`IngestView.vue`、`VectorLibraryConfigMerger` 四类锚点。

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 里程碑与需求
- `.planning/PROJECT.md` — 里程碑边界、Core Value、Out of Scope（结构化双轨、RAG 补丁）
- `.planning/REQUIREMENTS.md` — PIPE-01/02/03 及 traceability
- `.planning/ROADMAP.md` — Phase 1 目标、交付物、success criteria
- `.planning/STATE.md` — 当前焦点与进度

### 代码库地图（brownfield 锚点）
- `.planning/codebase/ARCHITECTURE.md` — 建库/入库/RAG 组件职责与数据流
- `.planning/codebase/CONCERNS.md` — 单轨向量、Excel 扁平化、预览不一致、lockPipeline 等已知问题
- `.planning/codebase/STRUCTURE.md` — 前后端目录与入口文件

### 后端关键类（流程文档必须对照）
- `knowbase-service/src/main/java/com/knowbase/library/config/VectorLibraryConfig.java` — 库级配置模型
- `knowbase-service/src/main/java/com/knowbase/library/service/LibraryConfigResolver.java` — 配置解析入口
- `knowbase-service/src/main/java/com/knowbase/library/config/VectorLibraryConfigMerger.java` — lockPipeline 合并逻辑
- `knowbase-service/src/main/java/com/knowbase/ingest/service/DocumentIngestor.java` — 上传入库入口
- `knowbase-service/src/main/java/com/knowbase/ingest/service/DocumentPipelineService.java` — 解析管道
- `knowbase-service/src/main/java/com/knowbase/vector/service/IndexingService.java` — 分块与嵌入
- `knowbase-service/src/main/java/com/knowbase/vector/chunk/IndexingChunkFilter.java` — 表头块过滤
- `knowbase-service/src/main/java/com/knowbase/vector/retrieval/ChunkMetadataBuilder.java` — chunk metadata（docType/mimeType）

### 前端关键入口
- `frontend/knowbase-ui/src/components/CreateLibraryWizard.vue` — 建库向导
- `frontend/knowbase-ui/src/components/EditLibrarySettingsDrawer.vue` — 库设置与 lockPipeline UI
- `frontend/knowbase-ui/src/views/IngestView.vue` — 采集、预览、overrideChunk
- `frontend/knowbase-ui/src/utils/libraryDefaults.js` — 默认配置形状

### Phase 1 计划交付（待创建）
- `.planning/docs/INGEST-PIPELINE.md` — 主流程文档（含 mermaid、决策树、配置矩阵）

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `LibraryConfigResolver` — 所有管道阶段已集中解析库级配置；文档须标注各 `*For(libraryId)` 方法对应阶段。
- `ChunkMetadataBuilder` — 已写入 `mimeType`/`docType`/`fileName`；可作为采集语义标签与检索过滤的文档样例字段。
- `CreateLibraryWizard` + `libraryDefaults.js` — 建库默认字段形状，决策树落点。
- `libraryConfig.js` `diffLibraryConfig` — Phase 5 配置 diff；Phase 1 文档可引用其字段路径命名。

### Established Patterns
- **库级 JSON 单源:** `vector_library.config_json` → `LibraryConfigResolver`；目标态文档在此基础上扩展「采集覆盖层」，不推翻模型。
- **异步管道:** 上传事务提交后 `DocumentPipelineService.scheduleProcessAfterCommit` → 解析 → `DocumentIndexCoordinator` → `IndexingService`。
- **预览 API 分叉:** `ParsePreviewService` / chunk-preview 与正式索引可能参数不一致——须在「当前差距」显式列出。

### Integration Points
- 建库：`VectorLibraryController` ← `CreateLibraryWizard`
- 采集：`DocumentController` / `UploadService` ← `IngestView`
- 配置变更：`VectorLibraryService` + `VectorLibraryConfigMerger`（lockPipeline）
- 质量验收锚点：chunk 列表 API + 检索预览（`RagRetrievalService`）用于文档中的样本说明，非 v1 自动化测试范围

</code_context>

<specifics>
## Specific Ideas

- 真实反模式样本：**杜鹏飞周报 xlsx**——错误设定或预览不一致导致运营困惑；正确设定下 4 chunk 全召回（用户已验证）。
- 通用库目标：MIME 自动默认（Excel→text-only+paragraph-first；扫描 PDF→OCR on）+ `documentMetadata` 语义标签（如 `semanticType=weekly-report`）供检索收窄，**不**在本阶段实现结构化双轨。
- 同质语义混合类型示例：周报 xlsx 与周报 pdf 导出可同垂直库；报销 xlsx 与周报 xlsx 应拆库（语义主轴）。
- 用户明确要求：**规划应合理前瞻，不局限于现状实现**；现状仅作差距附录。

</specifics>

<deferred>
## Deferred Ideas

- **结构化双轨 + QueryRouter** — `document_record`、表格枚举查询分流；PROJECT Out of Scope，另立里程碑。
- **v1 采集级 ingest profile 实现** — OCR/chunk 覆盖持久化、预览=入库工程落地 → Phase 4 + backlog。
- **MIME 自动默认引擎实现** — 目标态文档描述，实现可放 Phase 2/3 与预设联动。
- **软锁定 + 全库重索引任务** — 目标态文档描述，工程实现 backlog（替代现状 `VectorLibraryConfigMerger` 硬锁）。
- **RAG 答对率自动化验收** — 北极星指标，非本里程碑测试范围；可手工案例附录。
- **按文件类型质量细表** — Phase 2 `FILE-TYPE-PROCESSING.md`。
- **库预设 UI** — Phase 3 `libraryPresets.js` + 向导套用。

</deferred>

---

*Phase: 1-全链路流程梳理*
*Context gathered: 2026-06-10*
