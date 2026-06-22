# Ingestion 接口说明

本文说明 `feature-v1` 中入库链路新增的可替换接口。当前实现保持无外部运行时依赖，目标是先稳定解析、清洗、元数据抽取和切分之间的边界。

## 调用顺序

`SimpleIngestionPipeline` 按以下顺序执行：

1. `DocumentParser`：根据 `ParseRequest.mediaType`、`sourceName` 等信息选择解析器。
2. `DocumentCleaner`：在不改变块语义的前提下清洗文本。
3. `DocumentMetadataExtractor`：补充文档级统计与检索过滤元数据。
4. `DocumentChunker`：将清洗后的 `ParsedDocument` 切成检索用 `DocumentChunk`。

## 核心接口

### `DocumentParser`

位置：`com.knowbase.ingestion.parser.DocumentParser`

- `supports(ParseRequest request)`：判断解析器是否支持当前输入。
- `parse(ParseRequest request)`：将源内容解析为 `ParsedDocument`。
- `ParseRequest`：解析请求，包含 `documentId`、`content`、`mediaType`、`contentFamily` 和源元数据。

默认实现：

- `HtmlDocumentParser`：支持 `text/html`、`application/xhtml+xml`、`.html`、`.htm`，保留表格结构。
- `PlainTextDocumentParser`：支持 `text/plain`、`text/markdown`、`.txt`、`.md`，提供基础标题、段落、代码围栏解析。

### `DocumentCleaner`

位置：`com.knowbase.ingestion.cleaning.DocumentCleaner`

- `clean(ParsedDocument document, CleaningOptions options)`：返回清洗后的新 `ParsedDocument`。
- `CleaningOptions`：控制空白折叠、空行折叠和代码行尾清理。

默认实现：

- `WhitespaceDocumentCleaner`：清洗正文、FAQ、表格文本和代码行尾，保留 block 类型、表格坐标、表头继承和元数据。

### `DocumentMetadataExtractor`

位置：`com.knowbase.ingestion.metadata.DocumentMetadataExtractor`

- `extract(ParsedDocument document, MetadataExtractionOptions options)`：返回补充元数据后的新 `ParsedDocument`。
- `MetadataExtractionOptions`：控制是否写入块统计、文本统计和首标题。

默认实现：

- `DefaultDocumentMetadataExtractor`：写入 `blockCount.*`、`text.characterCount`、`text.tokenEstimate`、`firstHeading` 和 `contentFamily`。

### `DocumentChunker`

位置：`com.knowbase.ingestion.chunking.DocumentChunker`

- `chunk(ParsedDocument document, ChunkingOptions options)`：输出检索 chunk 列表。

默认实现：

- `SmartDocumentChunker`：继续提供 parent-child、sentence-window、table row group、FAQ pair、code symbol 和 PDF page metadata 路由。

## Pipeline Facade

位置：`com.knowbase.ingestion.pipeline.SimpleIngestionPipeline`

- `defaults()`：组装 HTML parser、纯文本 parser、whitespace cleaner、默认元数据抽取器和智能切分器。
- `ingest(ParseRequest request)`：使用默认配置执行完整流程。
- `ingest(ParseRequest request, IngestionOptions options)`：按调用方传入的清洗、元数据和切分配置执行。
- `IngestionResult`：包含 `parsedDocument`、`cleanedDocument`、`enrichedDocument` 和最终 `chunks`，便于调试和阶段审计。

## 后续扩展点

- 新解析器只需实现 `DocumentParser` 并加入 Pipeline parser 列表。
- 新清洗策略只需实现 `DocumentCleaner`，可接入 Profile 中的 `cleaning_config`。
- 新元数据策略只需实现 `DocumentMetadataExtractor`，可补充页码、章节、OCR 置信度等字段。
- 新切分策略只需实现 `DocumentChunker`，可按 `content_family` 或 `DocumentProfile` 路由。
