# 二期实施方案：复杂文档解析、召回评测与知识库治理增强

## 1. 背景

一期已经完成入库主链路：来源加载、Parser/Profile 路由、文本清洗、元数据增强、结构优先分段、token 预算约束、字符兜底、Embedding、索引写入和发布。

二期不替换现有 Java Pipeline，而是在现有 SPI 上增强复杂文档质量，并补齐知识库配置、召回测试、评测治理和文档生命周期等产品化能力，重点面向扫描件、复杂 PDF、Excel/CSV 报表、多栏文档、表格型知识、高质量引用和可运营的入库闭环。

## 2. 二期目标

1. 提升复杂 PDF 的阅读顺序、表格区域和引用坐标质量。
2. 接入真实 OCR 引擎输出，稳定保留 bbox、confidence、语言、旋转角和页内位置。
3. 强化表格语义模型，支持多级表头、合并单元格、公式值、隐藏行列、跨 sheet 引用。
4. 建立解析与切分评测集，用固定样本回归 chunk 边界、引用粒度和召回质量。
5. 固化外部解析器协议，支持按场景接入 Docling、Unstructured 或其他文档解析服务。
6. 产品化知识库 Profile 配置，支持创建后治理、版本对比、回滚和重建索引提示。
7. 增加知识库级召回测试与批量召回评测，支持在智能体发布前验证 topK、过滤、融合、重排和 citation 质量。
8. 补齐文档生命周期和数据源同步能力，支持文档删除、增量入库、单文档重建、目录/对象存储同步和索引清理策略。

## 3. 范围

### 3.1 PDF 深度解析

交付项：

- 基于页内坐标的阅读顺序模型。
- 多栏布局识别与列内顺序排序。
- 表格区域检测，输出 table region、row region、bbox、pageNumber。
- PDF chunk 保留 `sourcePage`、`bbox`、`readingOrder`、`columnIndex`、`tableRegionId`。
- 引用构造支持页码和 bbox。

验收标准：

- 多栏 PDF 样本的 chunk 顺序符合人工阅读顺序。
- 表格 PDF 样本能把表格行或区域独立切分。
- citation 能定位到页码和页内 bbox。

### 3.2 OCR 深度解析

交付项：

- 接入至少一种真实 OCR 输出格式，优先 hOCR/TSV/JSON。
- 块级保留 `ocrConfidence`、`bbox`、`pageNumber`、`language`、`rotation`。
- 扫描 PDF 与图片统一进入 `SCANNED_DOCUMENT` / `IMAGE_TEXT` Profile。
- 对低置信度 OCR chunk 增加过滤或降权字段。

验收标准：

- OCR 样本中每个结构块都有 confidence 与 bbox。
- 低置信度块可在检索后处理阶段识别。
- OCR 输出缺失坐标时，明确标记 `bboxSource=estimate` 或 `unavailable`。

### 3.3 表格语义增强

交付项：

- 表格块保留 sheet、row range、column range、header path、cell coordinates。
- 支持多级表头、合并单元格、公式值、隐藏行列、跨 sheet 引用。
- 建立表格 chunk 模板：table summary、row group、cell evidence。
- 表格引用支持 sheet name、row/column range 和 cell coordinate。

验收标准：

- Excel 报表样本能输出多级 header path。
- 合并单元格的 rowSpan/columnSpan 可检索和展示。
- 公式单元格保留公式与计算值。

### 3.4 语义切分与评测

交付项：

- 建立 `sample-documents` 回归集：PDF、扫描件、Excel、Markdown 长文、代码/配置。
- 增加 chunk 边界快照测试，固定结构段、token 数、chunk role 和 metadata。
- 增加检索评测脚本，覆盖召回、引用、表格证据和 OCR 低置信度样本。
- 可选引入 semantic chunk / sentence-window / parent-child profile。

验收标准：

- 每类样本至少覆盖 3 个代表文档。
- chunk 边界变更必须由快照测试显式暴露。
- 表格、PDF、OCR 样本均能回归 citation metadata。

### 3.5 外部解析器适配

交付项：

- 固化外部解析器 HTTP/JSON Schema。
- 支持 `parserCode=docling`、`parserCode=unstructured`、`parserCode=external`。
- 外部解析结果映射为统一 `ParsedDocument`、`StructuralBlock` 和 metadata。
- 外部服务失败时可配置 fallback 到 Java parser。

建议协议：

- 请求：原始文件 bytes、sourceUri、mimeType、parserCode、documentProfile。
- 响应：`text`、`blocks[]`、`tables[]`、`pages[]`、`images[]`、`metadata`。
- block 字段：`type`、`content`、`pageNumber`、`bbox`、`readingOrder`、`confidence`、`tableRegionId`。

验收标准：

- Docling/Unstructured mock 响应可稳定映射到 `ParsedDocument`。
- 外部解析器与 Java parser 共用同一 chunker。
- 外部解析器禁用时，默认 Java pipeline 不受影响。

### 3.6 知识库配置与 Profile 治理

交付项：

- 增加 `LibraryProfile` 版本列表、详情、版本对比、复制新版本、回滚和禁用能力。
- 增加 `DocumentProfile` 列表、创建、编辑、启停、复制、删除和差异对比能力。
- Profile 配置项覆盖 parser、cleaning、chunking、tokenizer、embedding、metadata schema、OCR/外部解析器参数和 retrievalTopK。
- Profile 变更时输出影响分析：受影响文档数、索引版本、是否需要重新入库、是否可仅重建增量。
- 前端增加知识库 Profile 管理页和变更确认流程。

验收标准：

- 创建知识库后仍可独立调整 `LibraryProfile` 和 `DocumentProfile`，并生成可追踪版本。
- Profile 变更不会修改已发布索引版本的读取结果，除非用户显式触发重建或发布新版本。
- 前端能展示 Profile 差异，并提示重新入库或重建索引的影响范围。

### 3.7 召回测试与批量评测

交付项：

- 增加知识库级召回测试接口，支持 `libraryId`、`indexVersionId`、question、topK、metadata filter、contentFamilyWeights、fusion、rerank、dedup 和 score 明细。
- 保留现有智能体级 retrieval test，并增加与知识库级测试共享的结果模型：raw candidates、fused candidates、reranked candidates、evidence、citation 和 context token。
- 建立批量召回评测数据集格式，样本包含 question、expectedDocumentIds、expectedChunkIds、expectedCitationMetadata、tags 和难度等级。
- 增加批量评测指标：recall@k、precision@k、MRR、nDCG、citation completeness、evidence coverage、table/OCR/PDF 专项命中率。
- 支持按 Profile 版本、索引版本、检索策略和模型配置做 A/B 对比。

验收标准：

- 用户无需创建智能体即可对单个知识库和指定索引版本做召回调试。
- 批量评测报告能定位失败样本、缺失 chunk、低分原因、策略差异和 citation metadata 缺口。
- 发布智能体或发布索引版本前，可引用最近一次召回评测结果作为质量门禁。

### 3.8 文档生命周期与数据源同步

交付项：

- 增加文档级查询、删除、停用、重新入库和单文档重建接口。
- 支持按 source URI、文件 hash、etag、lastModified 或外部 document key 做幂等 upsert。
- 增加目录、对象存储和外部数据源的同步任务抽象，支持全量扫描、增量扫描、dry run 和同步差异预览。
- 删除或停用文档时生成新索引版本，并明确清理旧 chunk、embedding、artifact 和引用缓存的策略。
- 前端增加文档生命周期操作入口，展示文档来源、版本、最近入库任务、当前索引状态和可重建原因。

验收标准：

- 同一文档重复入库能识别未变化、内容更新、来源删除和 Profile 变更四类情况。
- 删除或停用文档后，新发布索引不再召回对应 chunk，旧已发布索引仍遵守版本兼容策略。
- 同步任务能输出新增、更新、删除、跳过和失败文档明细。

## 4. 缺项与后续完善项

当前分支已经完成入库主链路与复杂文档有限增强，但与 Docling、Unstructured、RAGFlow 等深度文档解析能力相比，仍需在二期继续补齐以下缺项。

### 4.1 PDF 表格精准识别

现状：

- 已有基于 TextPosition 的行聚类、bbox、readingOrder、多栏和 table region 启发式。

不足：

- 表格检测仍以文本行间距和空白特征为主，不能稳定识别复杂网格线、跨页表格、无边框表格和嵌套表格。
- table region 尚未输出完整表格对象、单元格 bbox、列边界和跨页续表关系。

后续完善：

- 增加表格区域检测器，支持 ruled table、stream table 和跨页 continuation。
- 输出 table、row、cell 三层结构，保留 bbox、rowSpan、columnSpan、headerPath。
- 支持按 `tableRegionId` 构造 citation，并在前端定位页内区域。

### 4.2 OCR 引擎深度集成

现状：

- 已支持 hOCR 中的 bbox 与 confidence 解析，并在缺失时保留估算/不可用标记。
- 已支持 Tesseract 默认引擎与 PaddleOCR HTTP 适配（`paddleOcrEndpoint`）。
- **新增**：复杂/扫描 PDF 可配置官方 PaddleOCR-VL HTTP pipeline 或 vLLM OpenAI 接口（`knowbase.vision-document`）；亦可回退 Ollama 社区版 VLM。

配置（`application.yml`）：

```yaml
knowbase:
  vision-document:
    enabled: true
    provider: paddleocr-vl   # paddleocr-vl | vllm | ollama
    timeout: 600s
    paddleocr-vl:
      base-url: http://localhost:8888
    vllm:
      base-url: http://localhost:8118
      model: PaddleOCR-VL-1.6-0.9B
  ollama:
    vision-language-model: ""   # 官方服务启用时留空
  ingestion:
    pdf:
      vl-on-scanned: true
      vl-on-low-confidence: true
      vl-low-confidence-threshold: 0.55
      vl-fallback-to-heuristic: true
      vl-max-pages: 0
```

Docker 部署见 [PADDLEOCR_VL_DEPLOYMENT.md](./PADDLEOCR_VL_DEPLOYMENT.md)。

路由逻辑（`PdfLayoutParser`）：

1. 扫描件 / 显式 `pdfParseMode=vl|vision|paddleocr-vl` → VLM（若已配置）
2. VLM 失败且 `vl-fallback-to-heuristic=true` → Tesseract OCR 或 layout 启发式
3. 电子版 PDF layout 置信度低于阈值 → 可选 VLM 重解析

启用前：

- **官方 pipeline**：`PaddleOCR/deploy/paddleocr_vl_docker/hps` → 映射宿主机 **8888** → `docker compose up`
- **独立 vLLM**：`docker compose -f infra/docker-compose.paddleocr-vl.yml up -d`（端口 8118）
- **Ollama 回退**：`ollama pull MedAIBase/PaddleOCR-VL:0.9b`，`provider: ollama`

现状：

- 已支持 hOCR 中的 bbox 与 confidence 解析，并在缺失时保留估算/不可用标记。
- 已支持 Tesseract 默认引擎与 PaddleOCR HTTP 适配（`paddleOcrEndpoint`）。
- 复杂/扫描 PDF 可配置官方 PaddleOCR-VL HTTP pipeline 或 vLLM OpenAI 接口（`knowbase.vision-document`）；亦可回退 Ollama 社区版 VLM。
- **`LayoutAnalysisService`** 统一光栅页版面分析；**`PaddleOcrVlPrunedResultMapper`** 将 `prunedResult` bbox 写入块 metadata。
- **`OcrEngineAdapter` SPI**、Profile/`application.yml` 级 OCR 选项、**`OcrDownweightMode`** 降权闭环已落地。
- **`ReadingOrderHttpClient`** 支持专用阅读顺序 HTTP 端点（`knowbase.ingestion.reading-order.endpoint`）。
- **`EvidenceArtifactGenerator`** 可选将 PDF 页 PNG 写入 ObjectStorage（`evidence-artifacts.enabled`）。
- **`default_scanned_document`** preset 已切换为 `pdf-layout` + VLM/OCR 路由。

不足：

- 专用 reading-order 模型需自行部署 HTTP 服务；跨页/跨栏全局序号仍弱。
- 复杂 PDF 表格（嵌套/无边框网格）检测仍依赖启发式。

后续完善：

- 部署 PP-DocLayout 类 reading-order 端点并接入默认配置。
- 表格 cell 级 bbox 精度与 nested table 检测。
- Parser 健康探针（VLM/OCR endpoint ready check）。

### 4.3 Markdown / VLM 表格语义

现状：

- Markdown 解析器支持 GFM 管道表格（`| col |`），输出带 `tableRegionId`、`rowRole` 的 `table_row`。
- VLM（PaddleOCR-VL）输出的 Markdown 表格复用同一解析路径。
- `TableRegionIdParseEnricher` 为 OCR/VLM 等缺失 region 的连续 `table_row` 自动补全 `tableRegionId`，并触发 `table_summary` 注入。

不足：

- 合并单元格、嵌套表、无边框表在 Markdown/VLM 路径仍依赖启发式。
- HTML `colspan`/`rowspan` 已有 extractor，复杂嵌套表待加强。

后续完善：

- HTML/DOCX 浮动表与页内嵌套表区域检测。
- 表格 cell 级 bbox（PDF/VLM 路径）。

### 4.4 外部解析器协议固化

现状：

- 已有可选外部解析器 HTTP adapter，可接 Docling/Unstructured 风格服务。

不足：

- 请求/响应 JSON Schema 尚未版本化。
- 认证、超时、重试、fallback、错误码和可观测字段尚未规范。
- 外部解析器返回的 table/page/image/citation 坐标尚未完整映射。

后续完善：

- 定义 `external-parser.schema.json`，包含 `text`、`blocks`、`tables`、`pages`、`images`、`metadata`。
- 增加 adapter 级超时、重试、熔断和 fallback 到 Java parser。
- 将外部解析 trace 写入 `IngestionRun` 阶段轨迹。

### 4.5 语义切分与评测集

现状：

- 已有结构优先、token 预算、字符兜底，以及单元级自动化测试。
- **`sample-documents` 回归集**：test resources + 顶层 manifest，每类 ≥3 样例（`SampleDocumentCatalogCoverageTest` 门禁）。
- **chunk 边界快照**：`SampleDocumentChunkSnapshotTest` 固定 Markdown/CSV/OCR 的 indexable 块数与 token 上限。
- **离线 citation 评分**：`IngestionCitationCompletenessEvaluator` + `IngestionEvalReportGenerator`。
- **eval 脚本**：`run-ingestion-eval.ps1` 跑 parse/chunk 回归并输出 `sample-documents/ingestion-eval-report.json`。

不足：

- 在线 E2E hit@k 仍依赖 `verify-sample-documents.ps1` 对运行中后端。
- semantic chunk、sentence-window、parent-child profile 还不是可评测的产品预设。

后续完善：

- 将离线 citation 评分接入 CI 产物对比（基线 diff）。
- 补充 PDF/XLSX programmatic 样例的 chunk 边界快照。
- 增加在线召回 hit@k 报告与离线 citation 报告合并视图。

### 4.6 Citation 坐标闭环

现状：

- 解析和 chunk metadata 已逐步保留 page、bbox、table/cell 坐标。
- 文档详情页与 QA 页均已支持 PDF.js bbox overlay（`PdfPreviewPanel`）。

不足：

- Excel 预览仍无法按 cell 定位；多页/跨 sheet 引用策略未产品化。

后续完善：

- Excel/Word 预览定位与 cell 级 citation 展示。
- 问答上下文裁剪时保留 citation metadata 一致性。

### 4.7 多模态证据资产

现状：

- 当前以文本 chunk 和结构 metadata 为主。

不足：

- 尚未保留 PDF 页截图、表格区域截图、图片 OCR 原图区域等多模态证据资产。

后续完善：

- 为 PDF/OCR/table region 生成可选 artifact 引用。
- 在 evidence pack 中保留图片/表格区域 asset URI。
- 前端支持引用处预览页截图或表格区域截图。

### 4.8 知识库与 Profile 管理

现状：

- 已支持知识库创建、分页查询、删除，以及按库类型预设生成默认 `LibraryProfile` 与 `DocumentProfile`。

不足：

- Document Profile 仍主要在建库和入库时隐式使用，缺少独立 CRUD、启停、版本历史和差异对比。
- Library Profile 缺少版本历史、复制、回滚和变更影响提示。
- 前端缺少面向租户/知识库维度的 Profile 配置台账。
- Profile 变更与索引版本、入库任务、召回评测之间缺少明确联动。

后续完善：

- 增加 Document Profile 列表、创建、编辑、启停、复制和删除接口。
- 增加 Library Profile 版本列表、版本对比、复制为新版本和回滚能力。
- 前端增加 Profile 管理页，展示 parser、cleaning、chunking、tokenizer、metadata schema 配置。
- 变更 Profile 时提示是否需要创建新索引版本或重新入库。
- Profile 保存后可触发 chunk 预览、单文档试切和知识库级召回测试，作为发布前验证入口。

### 4.9 入库任务运维

现状：

- 已支持创建入库任务、查看单个任务状态、轮询终态和查看失败错误。

不足：

- 缺少入库任务列表、按状态/时间/知识库筛选、任务取消、失败文档重试。
- 失败恢复粒度仍偏粗，尚未支持复用解析/清洗/分段中间产物。
- 缺少重建索引、仅发布已有索引版本、删除草稿索引等运维动作。

后续完善：

- 增加 IngestionRun 列表与筛选接口。
- 支持取消运行中的任务、重试失败文档、跳过失败文档继续发布。
- 将解析、清洗、分段中间产物作为可选调试/重试缓存。
- 前端增加入库任务运维页，支持任务列表、失败重试、错误导出和索引版本跳转。

### 4.10 Pipeline 可观测与审计

现状：

- 已有 Pipeline span、错误记录和观测页面基础能力。
- **结构化应用日志**：入库与准备链路在 `DefaultIngestionPipeline`、`DocumentPreparationPipeline`、`IngestionStageTracer` 等类输出中文 SLF4J 日志，覆盖任务级、文档级、向量化/索引写入与单阶段耗时；消息格式与 grep 示例见 [INGESTION_INTERFACES.md](./INGESTION_INTERFACES.md) §结构化日志。

不足：

- 入库页与观测 trace 尚未深度联动。
- 缺少阶段耗时、文档级失败分布、parser/chunker/tokenizer 统计和吞吐量指标（应用日志已有单阶段 `durationMs`，尚未聚合为指标）。
- 缺少面向运维的告警与慢任务识别。

后续完善：

- 为 `LoadSource`、`ParseDocument`、`NormalizeText`、`ExtractMetadata`、`ChunkDocument`、`EmbedChunks`、`WriteIndex`、`PublishIndexVersion` 增加阶段耗时与计数指标。
- 入库任务详情页展示阶段瀑布图、失败文档分布、平均 token/chunk 数和 embedding 耗时。
- 支持按 runId 跳转观测 trace，并从观测页反查知识库、文档和索引版本。
- 增加慢任务、失败率和低 OCR 置信度告警指标。

### 4.10 知识库级召回测试

现状：

- **已有**库级召回测试页：`/libraries/:libraryId/retrieval-test`（`LibraryRetrievalTestPage.vue`），支持单库调试与黄金集 CRUD。
- 已有智能体级检索测试，可验证多库路由、检索、证据构建和上下文 token 拼装。

不足：

- 缺少 raw/fusion/rerank 分阶段候选对比、metadata filter 命中解释和分数组成明细（部分字段已有，产品化不足）。
- 缺少面向产品运营的召回测试历史记录、策略快照和失败样本沉淀。
- 发布索引前的 **promote-eval-gate** 默认关闭（`promote-eval-gate-enabled: false`）。

后续完善：

- 增加 `POST /api/v1/libraries/{libraryId}/retrieval-tests`，支持指定索引版本、topK、过滤条件、策略覆盖和 tokenizer/context 预算。
- 返回 raw candidates、fused candidates、reranked candidates、evidence、citation、vectorScore、keywordScore、rerankScore 和 trace。
- 前端增加单库召回测试页，支持保存测试用例、复制为批量评测样本、对比不同 Profile/索引版本。

### 4.11 批量召回评测与质量门禁

现状：

- 已有观测与问答评测接口基础，可记录 eval run 和样本结果。

不足：

- 当前评测偏问答结果，缺少针对入库与检索质量的标准召回指标。
- 缺少带标注 document/chunk/citation 的固定评测集和版本化报告。
- 缺少发布索引或智能体前的质量门禁机制。

后续完善：

- 定义 retrieval eval dataset schema，支持 expectedDocumentIds、expectedChunkIds、expectedCitationMetadata、tags、difficulty。
- 增加 recall@k、precision@k、MRR、nDCG、citation completeness、evidence coverage 和专项样本命中率。
- 支持评测报告按 library、indexVersion、profileVersion、agentVersion、retrievalPolicy 和 embeddingModel 维度聚合。
- 支持质量阈值配置，未达到阈值时阻止或警告索引发布、智能体发布。

### 4.12 文档生命周期与数据源同步

现状：

- 已支持上传、目录展开、批量 source URI 入库、文档与 chunk 查询。

不足：

- 缺少文档级删除、停用、重新入库、单文档重建和增量 upsert。
- 缺少数据源连接器、同步任务、dry run、变更预览和同步历史。
- 文档删除后 chunk、embedding、artifact、citation cache 与旧索引版本之间的清理策略仍需明确。

后续完善：

- 增加文档生命周期接口：查询来源、删除/停用、重新入库、单文档重建、按 source key 查找。
- 增加 source connector 抽象，覆盖本地目录、对象存储、HTTP/Sitemap、Git/代码仓库和业务系统导出。
- 使用 hash、etag、lastModified、externalDocumentKey 识别新增、更新、删除和未变化文档。
- 删除和增量更新均通过新索引版本发布，确保已发布版本可回滚、可审计。

## 5. 实施顺序

1. 定义外部解析器响应 Schema 与样例。
2. 完成 OCR hOCR/TSV/JSON 解析器和 confidence/bbox 映射。
3. 增强 PDF 表格区域检测与多栏阅读顺序。
4. 完成 Excel 多级表头、公式、隐藏行列、跨 sheet 引用。
5. 建立样本文档回归集与 chunk 边界快照。
6. 接入可选 Docling/Unstructured adapter。
7. 补齐 Document Profile 与 Library Profile 管理能力。
8. 增加知识库级召回测试，并与智能体级 retrieval test 复用结果模型。
9. 建立批量召回评测数据集、指标和质量门禁。
10. 补齐文档删除、增量 upsert、单文档重建和数据源同步任务。
11. 补齐入库任务列表、取消、失败重试和索引版本运维能力。
12. 建立 Pipeline 阶段耗时、失败分布与 runId trace 联动。
13. 将高质量 citation metadata 接入证据构造与前端展示。

## 6. 非目标

- 二期不训练自研 OCR 或版面模型。
- 二期不强制所有部署环境安装 Docling/Unstructured。
- 二期不改变已发布索引版本的读取兼容性。
- 二期不替换现有 Java 入库主链路。
- 二期不实现所有第三方 SaaS 数据源的深度双向同步，优先提供可扩展 connector 抽象和只读导入能力。

## 7. 风险与约束

- 深度 PDF 与 OCR 对 CPU、内存和外部依赖要求更高，需要 Profile 级开关。
- 外部解析器输出格式差异较大，需要稳定 Schema 做隔离。
- bbox 和 reading order 会影响 citation 展示，必须有快照测试约束。
- OCR confidence 不同引擎口径不一致，需要记录 `confidenceSource`。
- Profile 与入库任务运维会影响已发布索引版本，必须明确版本兼容和回滚策略。
- 知识库级召回测试和批量评测会暴露不同模型、索引版本、Profile 版本的质量差异，需要保存策略快照和测试数据版本。
- 文档删除、增量同步和单文档重建会影响索引一致性，需要明确软删除、旧索引保留、artifact 清理和审计边界。
