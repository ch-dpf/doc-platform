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

默认实现覆盖 Markdown/TXT、HTML、PDF、Word、Excel/CSV、PPT、OCR、QA、ZIP 和 Tika fallback；表格类默认使用 `table-deep`，将 CSV/Excel 行解析为 `table_row` 结构块。新增解析器只需实现该接口并加入 `DocumentSourceLoader` 的 parser 列表。

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

## 自动配置扩展

`knowbase-autoconfigure` 默认注册：

- `DocumentTextNormalizer`
- `DefaultDocumentMetadataEnricher`
- `DocumentPreparationPipeline`

宿主服务可以覆盖 `DocumentNormalizer` 或 `DocumentMetadataEnricher` Bean，自定义清洗规则、业务元数据、OCR 置信度、页码、章节、权限标签或租户字段。
