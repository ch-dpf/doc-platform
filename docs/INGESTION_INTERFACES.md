# Ingestion 接口说明

本文说明 `feature-v1` 中 `knowbase-ingestion` 模块的入库接口。当前链路按解析、清洗、元数据增强、切分四段组织，并通过 Spring Boot 自动配置暴露可替换 Bean。

## 调用顺序

`DocumentPreparationPipeline` 按以下顺序执行：

1. `DocumentSourceLoader`：加载 inline、file、MinIO 或展开后的子文档来源。
2. `DocumentParser`：按 `sourceUri` 与 `mimeType` 解析为 `ParsedDocument`。
3. `DocumentNormalizer`：对文本和结构块做清洗，返回 `NormalizationResult`。
4. `DocumentMetadataEnricher`：补充文档级统计、Profile 与策略上下文。
5. `DocumentChunker`：按语义/结构边界优先、token 预算约束、递归字符切分兜底的默认策略，将归一化并增强后的 `ParsedDocument` 切成 `DocumentChunk`。

## 上传与存储语义

- 上传文件统一写入 `ObjectStorage` 抽象。
- 默认 `knowbase.storage.type=local`，对象会写入本地文件系统对象存储根目录。
- 配置 `knowbase.storage.type=minio` 时，对象会写入 MinIO，并返回 `minio://bucket/objectKey`。
- `POST /api/v1/libraries/{libraryId}/ingestion-runs/upload` 使用路径参数 `libraryId` 绑定目标知识库，multipart 中只传文件、Profile 和发布选项。
- 前端批量/文件夹上传会保留浏览器提供的相对路径作为 multipart filename，后端按对象 key 存储并返回 source URI。

## 核心接口

### `DocumentParser`

位置：`knowbase-ingestion/src/main/java/com/knowbase/ingestion/DocumentParser.java`

- `supports(String sourceUri, String mimeType)`：判断解析器是否支持当前来源。
- `parse(DocumentSource source)`：将加载后的内容解析为 `ParsedDocument`。

默认实现覆盖 Markdown/TXT、HTML、PDF、Word、Excel/CSV、PPT、OCR、QA、ZIP 和 Tika fallback；表格类默认使用 `table-deep`（自适应三阶段：表头提升 / 表单元数据 / 坐标回退），将 Excel/CSV 行解析为带 `rowRole` 的 `table_row` 结构块。详见 `docs/EXCEL_ADAPTIVE_PARSE.md`。新增解析器只需实现该接口并加入 `DocumentSourceLoader` 的 parser 列表。

**PDF 视觉语言模型**：通过 `knowbase.vision-document` 接入官方 PaddleOCR-VL（`POST /layout-parsing`）或 vLLM（`POST /v1/chat/completions`）；`provider=ollama` 时回退 `knowbase.ollama.vision-language-model`。`PdfLayoutParser` 对扫描件或低置信度 layout 优先 VLM 逐页解析；未配置或调用失败时回退启发式 layout / Tesseract OCR。详见 [PADDLEOCR_VL_DEPLOYMENT.md](./PADDLEOCR_VL_DEPLOYMENT.md)、`docs/PHASE2_INGESTION_PLAN.md` §4.2。

### `DocumentNormalizer`

位置：`knowbase-ingestion/src/main/java/com/knowbase/ingestion/DocumentNormalizer.java`

- `normalize(ParsedDocument parsed, DocumentProfile documentProfile)`：执行清洗并返回 `NormalizationResult`。
- `normalizeText(String text)`：提供单段文本清洗能力。

默认实现 `DocumentTextNormalizer` 已实现该接口，支持控制字符、零宽字符、HTML entity、全角空格、页脚页码、项目符号、软连字符、重复标点和多余空行清理。

### `DocumentMetadataEnricher`

位置：`knowbase-ingestion/src/main/java/com/knowbase/ingestion/DocumentMetadataEnricher.java`

- `enrich(ParsedDocument document, MetadataContext context)`：返回补充元数据后的新 `ParsedDocument`。
- `MetadataContext`：携带 `sourceUri`、`libraryId`、`documentId`、`indexVersionId`、`LibraryProfile`、`DocumentProfile` 与来源选项。

默认实现 `DefaultDocumentMetadataEnricher` 写入 `metadataEnricher`、`contentFamily`、`structureAware`、`blockCount`、`textLength`、`firstHeading`、Profile ID、parser code、chunking strategy 和 token/chunk 配置。

### `DocumentChunker`

位置：`knowbase-ingestion/src/main/java/com/knowbase/ingestion/DocumentChunker.java`

- `chunk(UUID libraryId, UUID documentId, ParsedDocument document)`：基础切分入口。

默认实现 `TokenBasedDocumentChunker` 先由 `StructureSegmenter` 生成标题、页码、表格行、代码块、DOM 块等候选语义段，再使用模型 tokenizer 做 token 窗口约束；超长结构段先按递归字符分隔符切分后再进入 token 窗口，避免仅按字符数作为主策略。

## Pipeline Facade

位置：`knowbase-ingestion/src/main/java/com/knowbase/ingestion/DocumentPreparationPipeline.java`

- `prepare(...)`：完整执行加载、解析、结构增强、清洗、元数据增强和切分。
- `prepareFromParsed(...)`：从已解析文档继续执行清洗、元数据增强和切分。
- `parse(...)`：只执行加载与解析。
- `normalize(...)`：只执行清洗。

`PreparationStage` 支持 `PARSE`、`NORMALIZE`、`CHUNK`、`ALL`，用于预览、调试和分阶段验证。

REST 层通过 `IngestionRunController` 暴露对应端点（详见 [API.md](./API.md) §3.10–3.11）：

| HTTP | prepareStage |
|------|----------------|
| `POST .../ingestion/prepare/parse` | `parse` |
| `POST .../ingestion/prepare/normalize` | `normalize` |
| `POST .../ingestion/prepare/chunk` | `chunk` |
| `POST .../ingestion/prepare` | `all` |
| `POST .../ingestion/preview` | 预览切块，不写索引 |

前端入库向导第二步调用 `prepare/chunk` 展示分段预览，确认后通过 `ingestion-runs/upload` 或 `ingestion-runs` 正式入库。

## 结构化日志

入库与准备链路在关键阶段输出 **中文 SLF4J 结构化日志**（`INFO` / `WARN`），参数占位符便于 `grep` 与日志平台检索。与观测 trace span 互补：span 写入 DB/观测 API，应用日志写入标准输出。

### 按链路分层

| 层级 | 类 | 典型日志前缀 |
|------|-----|-------------|
| 准备 API | `DefaultIngestionPrepareService` | `准备批次开始/完成`、`准备文档完成/失败` |
| 入库任务 | `DefaultIngestionPipeline` | `入库任务开始/完成/失败`、`入库文档开始/完成/失败` |
| 向量化与索引 | `DefaultIngestionPipeline` | `向量化开始/完成/失败`、`索引写入开始/完成/失败` |
| 阶段 trace | `IngestionStageTracer` | `入库阶段开始/成功/失败`（含 `durationMs`） |
| 文档加载 | `DocumentSourceLoader` | `文档加载开始/完成`、`文档解析路由` |
| 准备解析 | `DocumentPreparationPipeline` | `准备阶段解析开始/完成` |
| 解析增强 | `ParsedDocumentParseEnricher` | `解析增强完成` |
| PDF 版面 | `PdfLayoutParser` | `PDF 版面解析开始/完成/失败` |
| 规范化 | `DocumentTextNormalizer` | `规范化完成` |
| 分块 | `TokenBasedDocumentChunker` | `分块完成` |
| Web 异常 | `KnowbaseExceptionHandler` | `资源未找到`、`访问被拒绝`、`请求参数无效`、`业务规则校验失败`、`未处理异常` |

### 日志消息一览

**准备（`POST .../ingestion/prepare/*`）**

```
准备批次开始: libraryId={}, stage={}, documents={}, profileCode={}
准备文档完成: libraryId={}, stage={}, sourceUri={}, profileCode={}
准备文档失败: libraryId={}, stage={}, sourceUri={}
准备批次完成: libraryId={}, stage={}, sourceCount={}, succeeded={}, failed={}
```

**入库任务（`DefaultIngestionPipeline`）**

```
入库任务开始: runId={}, libraryId={}, documents={}, profileCode={}
入库文档开始: runId={}, sourceUri={}
向量化开始: runId={}, sourceUri={}, documentId={}, indexableChunks={}, provider={}, model={}
向量化完成: runId={}, sourceUri={}, vectors={}
向量化失败: runId={}, sourceUri={}, documentId={}
索引写入开始: runId={}, sourceUri={}, documentId={}, chunks={}
索引写入完成: runId={}, sourceUri={}, documentId={}, chunks={}
索引写入失败: runId={}, sourceUri={}, documentId={}
入库文档完成: runId={}, sourceUri={}, documentId={}, chunks={}, indexable={}
入库文档失败: runId={}, sourceUri={}, documentId={}
入库任务失败: runId={}, libraryId={}, succeeded={}, failed={}
入库任务完成: runId={}, libraryId={}, status={}, succeeded={}, failed={}, chunks={}
```

**单文档处理子阶段**

```
文档加载开始: sourceUri={}
文档解析路由: sourceUri={}, parser={}, mimeType={}, bytes={}
文档加载完成: sourceUri={}, parser={}, blocks={}, structureAware={}
准备阶段解析开始: sourceUri={}
准备阶段解析完成: sourceUri={}, blocks={}, structureAware={}
解析增强完成: sourceUri={}, parserCode={}, blocks={}, indexableBlocks={}, tableRegions={}, parseConfidence={}
PDF 版面解析开始: sourceUri={}
PDF 版面解析完成: sourceUri={}, route={}, blocks={}, mlLayout={}, parseConfidence={}
PDF 版面解析失败: sourceUri={}
规范化完成: sourceUri={}, rawChars={}, normalizedChars={}, blocks={}->{}, rules={}
分块完成: sourceUri={}, engine={}, chunkMode={}, chunks={}, indexable={}, embeddingModel={}
```

**阶段 tracer（`IngestionStageTracer`，与 span 同名阶段）**

```
入库阶段开始: stage={}, runId={}, sourceUri={}, attrs={}
入库阶段开始: stage={}, attrs={}
入库阶段成功: stage={}, runId={}, sourceUri={}, durationMs={}, attrs={}
入库阶段成功: stage={}, durationMs={}, attrs={}
入库阶段失败: stage={}, runId={}, sourceUri={}, durationMs={}
入库阶段失败: stage={}, durationMs={}
```

`stage` 取值与 Pipeline 阶段对应，例如 `load_source`、`parse_document`、`normalize_text`、`chunk_document` 等。

### 本地排查示例

```powershell
# 跟踪某次入库任务
java -jar knowbase-app/target/knowbase-app-1.0.0-SNAPSHOT.jar 2>&1 |
  Select-String "runId=<uuid>"

# 查看 PDF 版面解析与回退
Select-String "PDF 版面解析"

# 准备预览失败
Select-String "准备文档失败"
```

## 自动配置扩展

`knowbase-autoconfigure` 默认注册：

- `DocumentTextNormalizer`
- `DefaultDocumentMetadataEnricher`
- `DocumentPreparationPipeline`

宿主服务可以覆盖 `DocumentNormalizer` 或 `DocumentMetadataEnricher` Bean，自定义清洗规则、业务元数据、OCR 置信度、页码、章节、权限标签或租户字段。
