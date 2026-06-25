package com.knowbase.ingestion;

import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleMarkdownParseRegressionTest {

    @Test
    void markdownGuideProducesEnrichedStructureBlocks() throws Exception {
        byte[] bytes = SampleMarkdownParseRegressionTest.class.getClassLoader()
                .getResourceAsStream("sample-documents/markdown/guide.md")
                .readAllBytes();

        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(new MarkdownStructureParser().parse(new DocumentSource(
                "memory://guide.md",
                "guide.md",
                "text/markdown",
                new ByteArrayInputStream(bytes),
                Map.of()
        )));

        assertTrue(parsed.structureAware());
        assertNotNull(parsed.metadata().get("parseConfidence"));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "heading".equals(block.blockType())));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("sectionPath")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("evidenceAssetHint")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("indexableHint")));
    }

    @Test
    void markdownTableProducesTableSummaryAfterEnrichment() {
        String markdown = """
                ## Sales

                | Region | Amount |
                | --- | --- |
                | East | 100 |
                """;
        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(new MarkdownStructureParser().parse(new DocumentSource(
                "memory://sales.md",
                "sales.md",
                "text/markdown",
                new ByteArrayInputStream(markdown.getBytes()),
                Map.of()
        )));

        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_summary".equals(block.blockType())));
        assertTrue(parsed.blocks().stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .allMatch(block -> block.metadata().containsKey("tableRegionId")));
    }
}
