# 二期实施方案：复杂文档解析与分段质量增强

## 1. 背景

一期已经完成入库主链路：来源加载、Parser/Profile 路由、文本清洗、元数据增强、结构优先分段、token 预算约束、字符兜底、Embedding、索引写入和发布。

二期不替换现有 Java Pipeline，而是在现有 SPI 上增强复杂文档质量，重点面向扫描件、复杂 PDF、Excel/CSV 报表、多栏文档、表格型知识和高质量引用。

## 2. 二期目标

1. 提升复杂 PDF 的阅读顺序、表格区域和引用坐标质量。
2. 接入真实 OCR 引擎输出，稳定保留 bbox、confidence、语言、旋转角和页内位置。
3. 强化表格语义模型，支持多级表头、合并单元格、公式值、隐藏行列、跨 sheet 引用。
4. 建立解析与切分评测集，用固定样本回归 chunk 边界、引用粒度和召回质量。
5. 固化外部解析器协议，支持按场景接入 Docling、Unstructured 或其他文档解析服务。

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
      base-url: http://localhost:8080
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

- **官方 pipeline**：`PaddleOCR/deploy/paddleocr_vl_docker/hps` → `docker compose up`（端口 8080）
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

不足：

- 缺少真实样本文档回归集。
- 缺少 chunk 边界快照、召回质量、引用完整性、OCR 低置信度样本评估。
- semantic chunk、sentence-window、parent-child profile 还不是可评测的产品预设。

后续完善：

- 建立 `sample-documents` 数据集，覆盖 PDF、扫描件、Excel、Markdown 长文、代码/配置。
- 增加 chunk snapshot 测试，固定 `boundaryType`、tokenCount、metadata、parent/child 关系。
- 增加 ingestion eval 脚本，输出召回、引用、chunk 边界和证据完整性报告。

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

后续完善：

- 增加 Document Profile 列表、创建、编辑、启停、复制和删除接口。
- 增加 Library Profile 版本列表、版本对比、复制为新版本和回滚能力。
- 前端增加 Profile 管理页，展示 parser、cleaning、chunking、tokenizer、metadata schema 配置。
- 变更 Profile 时提示是否需要创建新索引版本或重新入库。

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

不足：

- 入库页与观测 trace 尚未深度联动。
- 缺少阶段耗时、文档级失败分布、parser/chunker/tokenizer 统计和吞吐量指标。
- 缺少面向运维的告警与慢任务识别。

后续完善：

- 为 `LoadSource`、`ParseDocument`、`NormalizeText`、`ExtractMetadata`、`ChunkDocument`、`EmbedChunks`、`WriteIndex`、`PublishIndexVersion` 增加阶段耗时与计数指标。
- 入库任务详情页展示阶段瀑布图、失败文档分布、平均 token/chunk 数和 embedding 耗时。
- 支持按 runId 跳转观测 trace，并从观测页反查知识库、文档和索引版本。
- 增加慢任务、失败率和低 OCR 置信度告警指标。

## 5. 实施顺序

1. 定义外部解析器响应 Schema 与样例。
2. 完成 OCR hOCR/TSV/JSON 解析器和 confidence/bbox 映射。
3. 增强 PDF 表格区域检测与多栏阅读顺序。
4. 完成 Excel 多级表头、公式、隐藏行列、跨 sheet 引用。
5. 建立样本文档回归集与 chunk 边界快照。
6. 接入可选 Docling/Unstructured adapter。
7. 补齐 Document Profile 与 Library Profile 管理能力。
8. 补齐入库任务列表、取消、失败重试和索引版本运维能力。
9. 建立 Pipeline 阶段耗时、失败分布与 runId trace 联动。
10. 将高质量 citation metadata 接入证据构造与前端展示。

## 6. 非目标

- 二期不训练自研 OCR 或版面模型。
- 二期不强制所有部署环境安装 Docling/Unstructured。
- 二期不改变已发布索引版本的读取兼容性。
- 二期不替换现有 Java 入库主链路。

## 7. 风险与约束

- 深度 PDF 与 OCR 对 CPU、内存和外部依赖要求更高，需要 Profile 级开关。
- 外部解析器输出格式差异较大，需要稳定 Schema 做隔离。
- bbox 和 reading order 会影响 citation 展示，必须有快照测试约束。
- OCR confidence 不同引擎口径不一致，需要记录 `confidenceSource`。
- Profile 与入库任务运维会影响已发布索引版本，必须明确版本兼容和回滚策略。
