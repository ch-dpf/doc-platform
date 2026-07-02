# 解析层实施清单（Wave 2 剩余项）

> 基于 [PHASE2_INGESTION_PLAN.md](./PHASE2_INGESTION_PLAN.md) §3.1–§3.5、§4.1–§4.6 与 [BUILTIN_DEEP_PARSE.md](./BUILTIN_DEEP_PARSE.md) §8.4 整理。  
> 目标：在**不替换 Java 入库主链路**前提下，把内置解析层从约 75–80% 补到可回归、可运维、Citation 基本闭环。

## 1. 范围与原则

| 项 | 说明 |
|----|------|
| **在范围内** | 内置 `DocumentParser`、`LayoutAnalysisService`、Parse Enricher 链、外接 adapter、评测回归、Citation metadata |
| **不在范围内** | 自研 OCR/版面模型训练；强制部署 Docling/Unstructured；semantic chunk / parent-child 产品预设（属切分策略，单列 backlog） |
| **实施原则** | ① 每项必须有快照或 eval 验收；② 复杂 PDF 走「ML/VLM → 启发式 fallback」；③ 外接 parser 与内置共用 `ParsedDocumentParseEnricher` + Chunker |

### 1.1 核心模块地图

```text
DocumentSourceLoader
  └─ DocumentParser（按 Profile 路由）
       ├─ PdfLayoutParser ──► LayoutAnalysisService
       │                        ├─ OllamaLayoutTableProvider
       │                        ├─ PaddleOcrVlLayoutProvider
       │                        └─ LocalPdfLayoutProvider（PDFBox TextPosition）
       ├─ StructuredTableDocumentParser ──► AdaptiveTableLayoutAnalyzer
       ├─ MarkdownStructureParser / HtmlStructureParser / DocxStructureParser / …
       └─ ExternalDocumentParser ──► ExternalParserResponseMapper
  └─ ParsedDocumentParseEnricher（统一增强链）
       └─ … → ReadingOrderParseEnricher → UniversalParseConfidenceAggregator
  └─ DocumentPreparationPipeline（normalize → chunk）
```

---

## 2. 工作包总览

| ID | 工作包 | 优先级 | Wave | 二期来源 | 粗估 |
|----|--------|--------|------|----------|------|
| **PL-01** | 复杂 PDF 表格（嵌套/ruled/跨页 + cell bbox） | P1 | W2 | §3.1, §4.1 | 1.5–2 周 |
| **PL-02** | 阅读顺序服务默认化（Ollama/HTTP + 跨页） | P1 | W2 | §3.1, §4.2 | 1 周 |
| **PL-03** | 结构化格式复杂表（HTML/DOCX/Markdown） | P2 | W2 | §4.3 | 1 周 |
| **PL-04** | 外接解析器契约硬化 + fallback 可观测 | P2 | W1/W2 | §3.5, §4.4 | 0.5–1 周 |
| **PL-05** | 评测回归门禁（PDF/XLSX 快照 + CI 基线） | P1 | W2 | §3.4, §4.4 | 1 周 |
| **PL-06** | Citation 坐标闭环（Excel cell + Word 区域） | P2 | W5 | §4.5, §4.6 | 1–1.5 周 |
| **PL-07** | 多模态证据资产产品化 | P3 | W5 | §4.6 | 0.5 周 |
| **PL-08** | Parser 健康探针 | P3 | W2 | §4.2 | 0.5 周 |

**建议实施顺序**：PL-05（基线）→ PL-01 → PL-02 → PL-04 → PL-03 → PL-06 → PL-07 → PL-08  
> PL-05 与 PL-01/02 可并行：先加 PDF/XLSX 快照占位，再改解析逻辑，最后用快照锁回归。

---

## 3. 详细工作包

### PL-01 复杂 PDF 表格

**目标**：ruled/stream/nested/跨页续表稳定输出 table → row → cell 三层结构，chunk 与 citation 可定位 cell。

| 维度 | 内容 |
|------|------|
| **现状** | `LayoutPdfTextExtractor` + `PdfTableLayoutAnalyzer` + `PdfTableRegionMerger` + `PdfTableCellExtractor` 已有行聚类与续表；`PdfNestedTableSegmenter` 初版；`OllamaLayoutTableProvider` 可 ML 检测；`TableGridParseEnricher` / `TableSemanticParseEnricher` 补 grid 与合并单元格摘要 |
| **缺口** | 复杂网格线/无边框/嵌套表不稳定；cell 级 bbox 不完整；ruled table 列边界弱 |

**改动模块**

| 层级 | 类 / 文件 | 动作 |
|------|-----------|------|
| PDF 提取 | `LayoutPdfTextExtractor` | 增强 table region flush、nested depth 传播 |
| 表格分析 | `PdfTableLayoutAnalyzer` | ruled/stream 分类阈值调优；列边界检测 |
| 嵌套表 | `PdfNestedTableSegmenter` | 完善嵌套 depth、子表 region 切分 |
| 续表 | `PdfTableRegionMerger` | 跨页 header 对齐、column span 继承 |
| Cell | `PdfTableCellExtractor` | 输出 cell bbox、columnSpan/rowSpan、headerPath |
| ML 回退 | `OllamaLayoutTableProvider` | 失败标记 + fallback `local-pdf-layout` |
| Enricher | `TableGridParseEnricher`, `TableSemanticParseEnricher` | 消费 cell 级 metadata |
| 入口 | `PdfLayoutParser` | 路由 low-confidence → VLM 或 ML table |

**验收标准**

- [ ] 多栏 PDF 样本（≥3）chunk 顺序与人工阅读一致（配合 PL-02）
- [ ] 表格 PDF 样本：每表有唯一 `tableRegionId`；DATA 行独立 chunk 或 row group
- [ ] 跨页续表：同一逻辑表共享 region 或显式 `continuationOf`
- [ ] 嵌套表样本：`nestedTableDepth` ≥ 1 时子表有独立 region
- [ ] citation metadata 含 `pageNumber`、`bbox`（cell 或 row）、`tableRegionId`
- [ ] 单测：`PdfNestedTableSegmenterTest`、`TableGridParseEnricherTest`、新增 ruled/nested 回归样例

**依赖**：无硬依赖；与 PL-02 联调阅读顺序。

---

### PL-02 阅读顺序服务默认化

**目标**：多栏/跨页 PDF 块级 `readingOrder` 稳定，Profile 可配置且默认可用。

| 维度 | 内容 |
|------|------|
| **现状** | `ReadingOrderParseEnricher` 编排；`ReadingOrderHttpClient` + `OllamaReadingOrderClient`；`CrossPageReadingOrderAdjuster`；配置 `knowbase.ingestion.reading-order.provider=ollama` |
| **缺口** | 专用模型需自部署；跨页全局序号仍弱；未纳入默认 preset |

**改动模块**

| 层级 | 类 / 文件 | 动作 |
|------|-----------|------|
| 编排 | `ReadingOrderParseEnricher` | provider 优先级：HTTP → Ollama → heuristic-bbox |
| HTTP | `ReadingOrderHttpClient` | 超时/重试；响应 schema 校验 |
| Ollama | `OllamaReadingOrderClient` | 对接 `knowbase-reading-order` Modelfile |
| 跨页 | `CrossPageReadingOrderAdjuster` | 跨页表/脚注/页眉干扰处理 |
| 配置 | `application.yml` + `DocumentProfile.options` | `readingOrderEndpoint`、`reading-order.provider` |
| 脚本 | `scripts/pull-ollama-layout-models.ps1` | 文档化 reading-order 模型拉取 |
| Preset | `knowbase-preset` 默认 Profile | 电子版 PDF 启用 ollama/heuristic 链 |

**验收标准**

- [ ] 双栏/三栏 PDF（≥2 样本）：块 `readingOrder` 单调且 columnIndex 正确
- [ ] 跨页文档：续表块 readingOrder 紧接上文
- [ ] Ollama/HTTP 不可达时：静默 fallback heuristic，日志含 `readingOrderSource=heuristic-bbox`
- [ ] 单测：`ReadingOrderParseEnricherTest`、`CrossPageReadingOrderAdjusterTest`、`ReadingOrderHttpClientTest`

**依赖**：Ollama 或自研 HTTP 端点（可选）；不阻塞 heuristic 路径。

---

### PL-03 结构化格式复杂表

**目标**：HTML/DOCX/Markdown 路径下合并单元格、嵌套表、浮动表区域可检索且 metadata 完整。

| 维度 | 内容 |
|------|------|
| **现状** | `HtmlStructureParser` 支持顶层表 colspan/rowspan；`MarkdownStructureParser` GFM 管道表；`DocxStructureParser` gridSpan/vMerge；`SampleHtmlMergedCellsParseRegressionTest` |
| **缺口** | 嵌套表独立 region；DOCX 浮动表/文本框/页眉页脚；Markdown 无边框表 |

**改动模块**

| 格式 | 类 | 动作 |
|------|-----|------|
| HTML | `HtmlStructureParser` | 嵌套 `<table>` 分区；浮动表启发式 |
| DOCX | `DocxStructureParser` | 浮动表、文本框、header/footer 策略（可配置 skip） |
| MD | `MarkdownStructureParser` | 合并单元格占位、多表 region |
| 共用 | `TableRegionIdParseEnricher`, `TableGridParseEnricher` | 与 PDF 路径 metadata 对齐 |

**验收标准**

- [ ] `sample-documents/html/merged-cells.html`：rowSpan/columnSpan 入 block metadata
- [ ] 新增 nested-table.html / docx 样本：独立 `tableRegionId`
- [ ] DOCX 页眉页脚默认 `indexableHint=false`（可 Profile 覆盖）
- [ ] 单测：`SampleHtmlMergedCellsParseRegressionTest` 扩展 + DOCX 快照

**依赖**：无。

---

### PL-04 外接解析器契约硬化

**目标**：Docling/Unstructured/自定义 HTTP 解析可版本化、可 fallback、可追踪。

| 维度 | 内容 |
|------|------|
| **现状** | `ExternalDocumentParser`、`ExternalParserResponseMapper`、`ExternalParserFallbackResolver`；`external-parser.schema.json` v1.0；mock：`mock-docling-response.json` |
| **缺口** | 请求 schema、认证、重试/熔断、IngestionRun trace、table/page/image 完整映射 |

**改动模块**

| 层级 | 类 / 文件 | 动作 |
|------|-----------|------|
| Schema | `schemas/external-parser.schema.json` | 补 request schema；error 码；schemaVersion 升级策略 |
| 客户端 | `ExternalDocumentParser` | 超时、重试、Bearer/API key |
| 映射 | `ExternalParserResponseMapper` | tables[]/pages[]/images[] → StructuralBlock |
| Fallback | `ExternalParserFallbackResolver` | 按 parserCode 映射内置 parser；记录 fallbackReason |
| 观测 | `IngestionStageTracer` / 结构化日志 | 阶段 `externalParseMs`、`fallbackUsed` |
| 测试 | `ExternalDocumentParserFallbackTest`, `ExternalParserResponseMapperTest` | mock + 契约测试 |

**验收标准**

- [ ] mock Docling 响应稳定映射 `ParsedDocument`，经 `ParsedDocumentParseEnricher` 后与内置 chunk 一致
- [ ] 外接超时/5xx → fallback Java parser，任务不整体失败（Profile 可配 `failOnExternalError`）
- [ ] 响应缺 bbox 时标记 `bboxSource=unavailable`
- [ ] Schema 变更需 bump `schemaVersion`，单测锁定样例 JSON

**依赖**：无；可与 PL-01 并行。

---

### PL-05 评测回归门禁

**目标**：解析/切分改动必须被快照与离线 eval 捕获；为 Wave 4 Recall@k 门禁打基础。

| 维度 | 内容 |
|------|------|
| **现状** | `sample-documents/` 金标集；`SampleDocumentCatalogCoverageTest`；`SampleDocumentChunkSnapshotTest`（MD/CSV/OCR）；`IngestionCitationCompletenessEvaluator`；`run-ingestion-eval.ps1` → `ingestion-eval-report.json` |
| **缺口** | PDF/XLSX programmatic 快照；CI 基线 diff；在线 hit@k 合并视图 |

**改动模块**

| 层级 | 类 / 脚本 | 动作 |
|------|-----------|------|
| 快照 | `SampleDocumentChunkSnapshotTest` | 新增 PDF 多栏、PDF 表格、XLSX 多级表头用例 |
| 解析回归 | `SamplePdfParseRegressionTest`, `SampleXlsxParseRegressionTest` | 块数、tableRegionId、readingOrder 断言 |
| 覆盖 | `SampleDocumentCatalogCoverageTest` | 5 类 × ≥3 样本门禁 |
| Eval | `IngestionEvalReportGenerator`, `IngestionCitationCompletenessEvaluator` | citation 字段完整率评分 |
| 脚本 | `scripts/run-ingestion-eval.ps1`, `scripts/run-parse-regression.ps1` | 输出 JSON 报告 |
| CI | `.github/workflows/*` 或本地门禁文档 | 基线 diff 阈值（可选） |
| 金标 | `sample-documents/retrieval-eval-samples.json` | 10–20 条手工 QA |

**验收标准**

- [ ] 每类文档（PDF/扫描/Excel/MD/代码配置）≥3 样本被 catalog 测试覆盖
- [ ] PDF/XLSX chunk 边界变更导致快照测试失败（ intentional break 需更新快照并 PR 说明）
- [ ] `run-ingestion-eval.ps1` 产出 citation 完整率 ≥ 基线（基线文件入库或 CI artifact）
- [ ] 金标 JSON 格式定稿，可手工调 `retrieval-test` API

**依赖**：PL-01/02 改动应在本包快照落地后合并，或同步更新快照。

---

### PL-06 Citation 坐标闭环（Excel cell + Word）

**目标**：检索/问答引用可定位到 Excel 单元格、Word 页内区域；与 PDF bbox overlay 同级体验。

| 维度 | 内容 |
|------|------|
| **现状** | 后端 block/chunk metadata 含 sheet、rowIndex、cellCoordinates；前端 `LibraryDocumentChunksPage.vue` Excel 仅 Sheet 级定位；`PdfPreviewPanel.vue` 支持 bbox overlay |
| **缺口** | Excel cell 高亮；Word 区域框选；跨 sheet 引用策略 |

**改动模块**

| 层级 | 类 / 文件 | 动作 |
|------|-----------|------|
| 后端 | `EvidenceAssetHintEnricher` | 统一 evidence 字段：sheet、row、col、cellRef |
| Chunk | `SmartTableDocumentChunker` 等 | 保留 cellCoordinates 到 chunk metadata |
| 前端 | `LibraryDocumentChunksPage.vue` | Excel：按 rowIndex/col 高亮单元格（SheetJS 或 DOM overlay） |
| 前端 | Word 预览组件 | 片段高亮 → 区域框（可先文本 scroll） |
| 前端 | `format.js` | citation 展示 sheet + cellRef |
| API | 检索/evidence 响应 | 确保 citation payload 含坐标字段 |

**验收标准**

- [ ] Excel chunk 点击「定位」：跳转 Sheet 并高亮对应 cell（±合并单元格范围）
- [ ] PDF citation：页码 + bbox overlay（已有，回归即可）
- [ ] Word：至少 scroll + 片段高亮；理想态页内 bbox（可分期）
- [ ] 低置信度 OCR chunk 在 QA 页有 visual 标记（Wave 5 W5-4）

**依赖**：PL-01/03 表格 metadata 稳定；属 Wave 5，可与 PL-05 并行。

---

### PL-07 多模态证据资产产品化

**目标**：可选生成 PDF 页 PNG、表区截图，写入 evidence pack。

| 维度 | 内容 |
|------|------|
| **现状** | `EvidenceArtifactGenerator` + `knowbase.ingestion.evidence-artifacts.enabled`；ObjectStorage 写入 |
| **缺口** | 表区裁剪图；前端预览 asset URI；evidence pack 字段规范 |

**改动模块**

| 层级 | 类 | 动作 |
|------|-----|------|
| 生成 | `EvidenceArtifactGenerator` | 表区 bbox 裁剪（依赖 PL-01 cell bbox） |
| 存储 | ObjectStorage 配置 | bucket、max-pages 文档化 |
| 检索 | evidence 构造 | assetUri 入 citation |
| 前端 | 引用卡片 | 页截图/表区缩略图 |

**验收标准**

- [ ] `evidence-artifacts.enabled=true` 时 PDF 样本 chunk 含 `pageAssetUri`
- [ ] 前端引用处可预览页图（可选折叠）
- [ ] 关闭开关时零存储副作用

**依赖**：PL-01（表区 bbox）可选；Wave 5。

---

### PL-08 Parser 健康探针

**目标**：启动或入库前检测 VLM/OCR/reading-order 端点可用，避免静默降级。

| 维度 | 内容 |
|------|------|
| **现状** | 无统一探针；VLM 失败在 `PdfLayoutParser` 运行时 fallback |
| **缺口** | 启动 check、Profile 保存时 warn、运维 API |

**改动模块**

| 层级 | 建议新建 | 动作 |
|------|----------|------|
| SPI | `ParserEndpointHealthChecker` | 检测 vision-document、ocr、paddle、reading-order |
| 集成 | `KnowbaseAutoConfiguration` | 可选 startup probe |
| API | `GET /api/v1/admin/parser-health`（或 observability 扩展） | 返回各 endpoint status |
| 日志 | 结构化日志 | `parserHealthCheck=degraded` |

**验收标准**

- [ ] VLM endpoint down 时：日志 + 可选 Actuator health 子状态
- [ ] 不影响无 ML 部署（全 heuristic 仍 GREEN）
- [ ] 单测：mock HTTP 200/503

**依赖**：无；P3 可后置。

---

## 4. 与二期 Wave 对照

| Wave 交付项 | 本清单 ID |
|-------------|-----------|
| W1-1 external-parser.schema + mock | PL-04 |
| W1-2 OCR hOCR/TSV/JSON | ✅ 已完成（`OcrHocrParser` 等） |
| W1-3 OcrEngineAdapter SPI | ✅ 已完成（`OcrEngineRegistry`） |
| W2-1 PDF 多栏 + 表格 | PL-01, PL-02 |
| W2-2 Excel 多级表头/公式 | ✅ 主体完成；PL-05 补快照 |
| W2-3 sample-documents + 快照 | PL-05 |
| W2-4 黄金问答集 | PL-05 |
| W2-5 Docling/Unstructured adapter | PL-04 |
| W5-3~5 Citation + 多模态 | PL-06, PL-07 |

---

## 5. 里程碑检查表

### M2（Wave 2 完成 — 解析质量可回归）

- [ ] PL-05 全绿：5 类样本 + PDF/XLSX 快照
- [ ] PL-01 + PL-02 验收项全过
- [ ] PL-04 mock adapter + fallback 可演示
- [ ] `ingestion-eval-report.json` 首版基线归档

### M5（二期完成 — Citation 闭环）

- [ ] PL-06 Excel cell 定位可用
- [ ] PL-07 页 PNG 可选开启
- [ ] PL-08 或等价运维可见性
- [ ] 黄金集 Recall@k 报告可归档（Wave 4 W4-6）

---

## 6. Backlog（非解析本体，二期末/三期）

| 项 | 说明 | 相关类 |
|----|------|--------|
| semantic chunk / sentence-window | 切分策略，非 parser | `DocumentChunker` SPI |
| parent-child profile | 检索层级 | retrieval 模块 |
| ONNX local layout provider | 降低 VLM 依赖 | `LayoutAnalysisProvider` 新实现 |
| 入库中间产物缓存 | 失败重试粒度 | `IngestionRun` 运维 §4.9 |
| JSON Schema 语义解析 | 配置类文档 | `code-config-structure` 扩展 |

---

## 7. 相关文档与命令

| 文档 | 用途 |
|------|------|
| [BUILTIN_DEEP_PARSE.md](./BUILTIN_DEEP_PARSE.md) | 架构与能力矩阵 |
| [PHASE2_INGESTION_PLAN.md](./PHASE2_INGESTION_PLAN.md) | 二期目标与缺项 |
| [EXCEL_ADAPTIVE_PARSE.md](./EXCEL_ADAPTIVE_PARSE.md) | table-deep 三阶段 |
| [INGESTION_INTERFACES.md](./INGESTION_INTERFACES.md) | SPI 与结构化日志 |

```powershell
# 离线 parse + chunk 回归
.\scripts\run-ingestion-eval.ps1

# 解析专项回归
.\scripts\run-parse-regression.ps1

# 后端单测（含快照）
mvn -Dmaven.repo.local=.m2/repository -pl knowbase-ingestion test
```

---

*版本：v0.1 | 2026-06-29 | 对齐当前 `knowbase-ingestion` 分支类名*
