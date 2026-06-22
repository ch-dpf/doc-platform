package com.knowbase.ingestion.pipeline;

import com.knowbase.ingestion.chunking.SmartDocumentChunker.ChunkKind;
import com.knowbase.ingestion.chunking.SmartDocumentChunker.ChunkingOptions;
import com.knowbase.ingestion.document.ParsedDocument.BlockType;
import com.knowbase.ingestion.document.ParsedDocument.ContentFamily;
import com.knowbase.ingestion.parser.DocumentParser.ParseRequest;
import com.knowbase.ingestion.pipeline.SimpleIngestionPipeline.IngestionOptions;
import com.knowbase.ingestion.pipeline.SimpleIngestionPipeline.IngestionResult;

import java.util.Map;

public final class SimpleIngestionPipelineSelfTest {

    public static void main(String[] args) {
        ingestsHtmlThroughDefaultPipeline();
        ingestsMarkdownThroughDefaultPipeline();
    }

    private static void ingestsHtmlThroughDefaultPipeline() {
        IngestionResult result = SimpleIngestionPipeline.defaults().ingest(
                new ParseRequest(
                        "pipeline-html",
                        """
                                <html><body>
                                <h1>Revenue report</h1>
                                <p>APAC   revenue improved. EMEA revenue stayed stable.</p>
                                <table>
                                  <caption>Revenue</caption>
                                  <tr><th>Region</th><th>Q1</th></tr>
                                  <tr><th scope="row">APAC</th><td>10</td></tr>
                                </table>
                                </body></html>
                                """,
                        "text/html",
                        ContentFamily.WEB_PAGE,
                        Map.of("sourceName", "report.html")
                ),
                new IngestionOptions(null, null, new ChunkingOptions(128, 256, 2, 1))
        );

        assertEquals("html-jdk", result.parsedDocument().metadata().get("parser"), "html parser selected");
        assertEquals("whitespace", result.cleanedDocument().metadata().get("cleaner"), "cleaner metadata");
        assertEquals("Revenue report", result.enrichedDocument().metadata().get("firstHeading"), "first heading metadata");
        assertEquals("1", result.enrichedDocument().metadata().get("blockCount.table"), "table block count");
        assertTrue(result.chunks().stream().anyMatch(chunk -> chunk.kind() == ChunkKind.TABLE_ROW_GROUP), "table row group chunk");
    }

    private static void ingestsMarkdownThroughDefaultPipeline() {
        IngestionResult result = SimpleIngestionPipeline.defaults().ingest(
                new ParseRequest(
                        "pipeline-md",
                        """
                                # Install

                                Prepare    the package. Verify the health check.

                                ```java
                                public class Demo {
                                  void run() {}
                                }
                                ```
                                """,
                        "text/markdown",
                        ContentFamily.RICH_TEXT,
                        Map.of("sourceName", "guide.md")
                ),
                new IngestionOptions(null, null, new ChunkingOptions(128, 256, 2, 1))
        );

        assertEquals("plain-text", result.parsedDocument().metadata().get("parser"), "markdown parser selected");
        assertTrue(result.enrichedDocument().blocks().stream().anyMatch(block -> block.type() == BlockType.HEADING), "heading parsed");
        assertTrue(result.enrichedDocument().blocks().stream().anyMatch(block -> block.type() == BlockType.CODE), "code fence parsed");
        assertTrue(result.chunks().stream().anyMatch(chunk -> chunk.kind() == ChunkKind.CODE_SYMBOL), "code chunk emitted");
        assertTrue(result.cleanedDocument().plainText().contains("Prepare the package."), "whitespace cleaned");
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
