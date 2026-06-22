# KnowBase RAG 平台

KnowBase 是面向内部知识管理场景的 RAG 平台，提供知识库建设、文档入库、向量检索、多知识库编排与智能问答能力。

项目采用 Java 21、Spring Boot、PostgreSQL、pgvector、MinIO、Apache Tika、Ollama、Vue 3、Vite 与 Element Plus 构建，支持独立运行和宿主服务引入两种形态。

当前设计文档：

- `docs/DESIGN.md`：KnowBase RAG 平台总体设计规划。

## 当前代码实现

- `src/main/java/com/knowbase/ingestion/document`：解析后的统一文档块模型，包含正文、标题、FAQ、代码与结构化表格块。
- `src/main/java/com/knowbase/ingestion/parser/DocumentParser.java`：解析器 SPI，使用 `ParseRequest` 承载内容、媒体类型、文档类型和源元数据。
- `src/main/java/com/knowbase/ingestion/parser/HtmlDocumentParser.java`：基于 JDK HTML parser 的 HTML 表格结构保真解析，保留 table block、cell 坐标、rowspan/colspan、scope、表头继承和表格摘要。
- `src/main/java/com/knowbase/ingestion/parser/PlainTextDocumentParser.java`：无依赖的纯文本/Markdown 基础解析器，支持标题、段落和代码围栏。
- `src/main/java/com/knowbase/ingestion/cleaning`：清洗阶段接口与默认 whitespace cleaner。
- `src/main/java/com/knowbase/ingestion/metadata`：文档元数据抽取接口与默认统计实现。
- `src/main/java/com/knowbase/ingestion/chunking/SmartDocumentChunker.java`：默认智能分段器，按“语义结构边界优先、token 预算约束、字符切分兜底”执行，并内置 sentence-window、parent-child、prev/next relation、FAQ pair、表格 row group、代码结构和 PDF page/section 混合策略入口。
- `src/main/java/com/knowbase/ingestion/pipeline/SimpleIngestionPipeline.java`：轻量入库编排，将解析、清洗、元数据抽取和切分串联为一个可测试流程。

OCR/PDF 的深度版面解析后续可通过同一 `ParsedDocument` 契约接入 Docling、MinerU、PaddleOCR、LayoutParser 或独立解析服务。

接口说明：

- `docs/INGESTION_INTERFACES.md`：解析、清洗、元数据抽取、切分和轻量 Pipeline 的接口职责与扩展点。

## 本地自检

当前核心实现无外部运行时依赖，可直接用 Java 21 编译并运行轻量自检：

```bash
rm -rf /tmp/knowbase-ingestion-classes
mkdir -p /tmp/knowbase-ingestion-classes
javac --release 21 -encoding UTF-8 -d /tmp/knowbase-ingestion-classes $(rg --files src/main/java src/test/java)
java -cp /tmp/knowbase-ingestion-classes com.knowbase.ingestion.chunking.SmartDocumentChunkerSelfTest
java -cp /tmp/knowbase-ingestion-classes com.knowbase.ingestion.pipeline.SimpleIngestionPipelineSelfTest
```

