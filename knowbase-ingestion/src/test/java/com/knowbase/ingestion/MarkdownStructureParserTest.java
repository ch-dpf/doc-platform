package com.knowbase.ingestion;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownStructureParserTest {

    private final MarkdownStructureParser parser = new MarkdownStructureParser();

    @Test
    void parsesHeadingsAndParagraphs() {
        String markdown = """
                # Title

                First paragraph.

                ## Section

                Second paragraph.
                """;
        ParsedDocument parsed = parser.parse(new DocumentSource(
                "memory://doc.md",
                "doc.md",
                "text/markdown",
                new java.io.ByteArrayInputStream(markdown.getBytes()),
                Map.of()
        ));

        assertTrue(parsed.structureAware());
        assertEquals(MarkdownStructureParser.PARSER_CODE, parsed.metadata().get("parserCode"));
        assertTrue(parsed.blocks().size() >= 4);
        assertTrue(parsed.blocks().stream().anyMatch(block -> "heading".equals(block.blockType()) && block.level() == 1));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "heading".equals(block.blockType()) && block.level() == 2));
    }

    @Test
    void parsesPipeTablesWithRegionMetadata() {
        String markdown = """
                # Metrics

                | Name | Score |
                | --- | --- |
                | Alpha | 10 |
                | Beta | 20 |
                """;
        ParsedDocument parsed = parser.parse(new DocumentSource(
                "memory://table.md",
                "table.md",
                "text/markdown",
                new java.io.ByteArrayInputStream(markdown.getBytes()),
                Map.of()
        ));

        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_row".equals(block.blockType())));
        assertTrue(parsed.blocks().stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .allMatch(block -> block.metadata().containsKey("tableRegionId")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.content().contains("Alpha")));
    }
}
