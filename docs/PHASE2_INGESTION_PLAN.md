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

## 4. 实施顺序

1. 定义外部解析器响应 Schema 与样例。
2. 完成 OCR hOCR/TSV/JSON 解析器和 confidence/bbox 映射。
3. 增强 PDF 表格区域检测与多栏阅读顺序。
4. 完成 Excel 多级表头、公式、隐藏行列、跨 sheet 引用。
5. 建立样本文档回归集与 chunk 边界快照。
6. 接入可选 Docling/Unstructured adapter。
7. 将高质量 citation metadata 接入证据构造与前端展示。

## 5. 非目标

- 二期不训练自研 OCR 或版面模型。
- 二期不强制所有部署环境安装 Docling/Unstructured。
- 二期不改变已发布索引版本的读取兼容性。
- 二期不替换现有 Java 入库主链路。

## 6. 风险与约束

- 深度 PDF 与 OCR 对 CPU、内存和外部依赖要求更高，需要 Profile 级开关。
- 外部解析器输出格式差异较大，需要稳定 Schema 做隔离。
- bbox 和 reading order 会影响 citation 展示，必须有快照测试约束。
- OCR confidence 不同引擎口径不一致，需要记录 `confidenceSource`。
