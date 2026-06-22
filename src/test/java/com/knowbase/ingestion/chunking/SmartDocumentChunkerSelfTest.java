package com.knowbase.ingestion.chunking;

import com.knowbase.ingestion.chunking.SmartDocumentChunker.ChunkKind;
import com.knowbase.ingestion.chunking.SmartDocumentChunker.ChunkingOptions;
import com.knowbase.ingestion.chunking.SmartDocumentChunker.DocumentChunk;
import com.knowbase.ingestion.document.ParsedDocument;
import com.knowbase.ingestion.document.ParsedDocument.BlockType;
import com.knowbase.ingestion.document.ParsedDocument.CodeBlock;
import com.knowbase.ingestion.document.ParsedDocument.ContentFamily;
import com.knowbase.ingestion.document.ParsedDocument.FaqBlock;
import com.knowbase.ingestion.document.ParsedDocument.TableBlock;
import com.knowbase.ingestion.document.ParsedDocument.TableCell;
import com.knowbase.ingestion.document.ParsedDocument.TextBlock;
import com.knowbase.ingestion.parser.HtmlDocumentParser;

import java.util.List;
import java.util.Map;

public final class SmartDocumentChunkerSelfTest {

    public static void main(String[] args) {
        parsesHtmlTablesWithInheritedHeaders();
        chunksTablesAsSummaryAndRowGroups();
        chunksTextWithParentChildAndRelations();
        routesFaqAndCodeFamilies();
    }

    private static void parsesHtmlTablesWithInheritedHeaders() {
        ParsedDocument document = new HtmlDocumentParser().parse("html-1", """
                <html><body>
                <h1>Quarterly report</h1>
                <table>
                  <caption>Revenue</caption>
                  <tr><th>Region</th><th>Q1</th><th>Q2</th></tr>
                  <tr><th scope="row">APAC</th><td>10</td><td>12</td></tr>
                </table>
                </body></html>
                """);

        TableBlock table = (TableBlock) document.blocks().stream()
                .filter(block -> block.type() == BlockType.TABLE)
                .findFirst()
                .orElseThrow();

        assertEquals(2, table.rowCount(), "row count");
        assertEquals(3, table.columnCount(), "column count");
        assertTrue(table.summary().contains("Revenue"), "summary contains caption");

        TableCell q1 = table.dataCells().stream()
                .filter(cell -> cell.value().equals("10"))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("Q1"), q1.inheritedHeaders().get("column"), "column header inheritance");
        assertEquals(List.of("APAC"), q1.inheritedHeaders().get("row"), "row header inheritance");
    }

    private static void chunksTablesAsSummaryAndRowGroups() {
        ParsedDocument document = new HtmlDocumentParser().parse("html-2", """
                <table>
                  <caption>Revenue</caption>
                  <tr><th>Region</th><th>Q1</th><th>Q2</th></tr>
                  <tr><th scope="row">APAC</th><td>10</td><td>12</td></tr>
                  <tr><th scope="row">EMEA</th><td>8</td><td>9</td></tr>
                </table>
                """);

        List<DocumentChunk> chunks = new SmartDocumentChunker().chunk(document, new ChunkingOptions(256, 512, 2, 1));
        DocumentChunk summary = chunks.stream()
                .filter(chunk -> chunk.kind() == ChunkKind.TABLE_SUMMARY)
                .findFirst()
                .orElseThrow();
        DocumentChunk rowGroup = chunks.stream()
                .filter(chunk -> chunk.kind() == ChunkKind.TABLE_ROW_GROUP)
                .findFirst()
                .orElseThrow();

        assertEquals(summary.chunkId(), rowGroup.parentChunkId(), "row group parent");
        assertTrue(rowGroup.text().contains("column=Q1"), "row group includes column headers");
        assertTrue(rowGroup.text().contains("row=APAC"), "row group includes row headers");
        assertEquals("table-row-group", rowGroup.metadata().get("strategy"), "table strategy metadata");
    }

    private static void chunksTextWithParentChildAndRelations() {
        ParsedDocument document = ParsedDocument.builder("text-1", ContentFamily.RICH_TEXT)
                .block(new TextBlock(BlockType.HEADING, "Install", ParsedDocument.metadata("level", "2")))
                .block(new TextBlock(BlockType.PARAGRAPH, "Prepare the package. Install the service. Verify the health check.", Map.of()))
                .block(new TextBlock(BlockType.PAGE_BREAK, "page break", ParsedDocument.metadata("pageNumber", "2")))
                .block(new TextBlock(BlockType.PARAGRAPH, "Page two starts a new section. It keeps the page metadata.", Map.of()))
                .build();

        List<DocumentChunk> chunks = new SmartDocumentChunker().chunk(document, new ChunkingOptions(64, 128, 2, 1));

        assertTrue(chunks.stream().anyMatch(chunk -> chunk.kind() == ChunkKind.PARENT), "parent chunks exist");
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.kind() == ChunkKind.SENTENCE_WINDOW), "sentence window chunks exist");
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.kind() == ChunkKind.PDF_PAGE_SECTION), "page section chunks exist");
        assertEquals(chunks.get(1).chunkId(), chunks.get(0).nextChunkId(), "next relation");
        assertEquals(chunks.get(0).chunkId(), chunks.get(1).previousChunkId(), "previous relation");
    }

    private static void routesFaqAndCodeFamilies() {
        ParsedDocument faq = ParsedDocument.builder("faq-1", ContentFamily.RICH_TEXT)
                .block(new FaqBlock("How to reset?", "Use the admin console reset action.", Map.of()))
                .build();
        ParsedDocument code = ParsedDocument.builder("code-1", ContentFamily.CODE_OR_CONFIG)
                .block(new CodeBlock("java", "public class Demo {\n  void run() {}\n}", Map.of()))
                .build();

        List<DocumentChunk> faqChunks = new SmartDocumentChunker().chunk(faq, new ChunkingOptions(64, 128, 2, 1));
        List<DocumentChunk> codeChunks = new SmartDocumentChunker().chunk(code, new ChunkingOptions(64, 128, 2, 1));

        assertEquals(ChunkKind.FAQ_PAIR, faqChunks.getFirst().kind(), "faq strategy");
        assertEquals(ChunkKind.CODE_SYMBOL, codeChunks.getFirst().kind(), "code strategy");
        assertEquals("code-ast-structural", codeChunks.getFirst().metadata().get("strategy"), "code metadata");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
