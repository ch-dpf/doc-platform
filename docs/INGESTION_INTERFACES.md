# Ingestion 接口说明

本文说明 `feature-v1` 中 `knowbase-ingestion` 模块的入库接口。当前链路按解析、清洗、元数据增强、切分四段组织，并通过 Spring Boot 自动配置暴露可替换 Bean。

## 调用顺序

`DocumentPreparationPipeline` 按以下顺序执行：

1. `DocumentSourceLoader`：加载 inline、file、MinIO 或展开后的子文档来源。
2. `DocumentParser`：按 `sourceUri` 与 `mimeType` 解析为 `ParsedDocument`。
3. `DocumentNormalizer`：对文本和结构块做清洗，返回 `NormalizationResult`。
4. `DocumentMetadataEnricher`：补充文档级统计、Profile 与策略上下文。
5. `DocumentChunker`：将归一化并增强后的 `ParsedDocument` 切成 `DocumentChunk`。

## 核心接口

### `DocumentParser`

位置：`knowbase-ingestion/src/main/java/com/knowbase/ingestion/DocumentParser.java`

- `supports(String sourceUri, String mimeType)`：判断解析器是否支持当前来源。
- `parse(DocumentSource source)`：将加载后的内容解析为 `ParsedDocument`。

默认实现覆盖 Markdown/TXT、HTML、PDF、Word、Excel/CSV、PPT、OCR、QA、ZIP 和 Tika fallback。新增解析器只需实现该接口并加入 `DocumentSourceLoader` 的 parser 列表。

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

默认实现 `TokenBasedDocumentChunker` 结合 `StructureSegmenter`、模型 tokenizer 和字符兜底切分，输出 parent-child、结构段、表格行组、代码块、FAQ 等检索 chunk。

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
