package com.knowbase.ingestion;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Test
    void parsesMultipleTablesWithDistinctRegionIds() {
        String markdown = """
                | A | 1 |
                | --- | --- |
                | x | 1 |

                | B | 2 |
                | --- | --- |
                | y | 2 |
                """;
        ParsedDocument parsed = parser.parse(source("multi-table.md", markdown));
        Set<Object> regions = parsed.blocks().stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .map(block -> block.metadata().get("tableRegionId"))
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(2, regions.size());
    }

    @Test
    void markdownMergePlaceholderMarksCellCoordinates() {
        String markdown = """
                | Name | Qty |
                | --- | --- |
                | Widget | 10 |
                | ^ | 3 |
                """;
        ParsedDocument parsed = parser.parse(source("merge.md", markdown));
        assertTrue(parsed.blocks().stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .anyMatch(block -> hasMergePlaceholder(block.metadata())));
    }

    private static DocumentSource source(String filename, String markdown) {
        return new DocumentSource(
                "memory://" + filename,
                filename,
                "text/markdown",
                new java.io.ByteArrayInputStream(markdown.getBytes()),
                Map.of()
        );
    }

    @SuppressWarnings("unchecked")
    private static boolean hasMergePlaceholder(Map<String, Object> metadata) {
        Object coordinates = metadata.get("cellCoordinates");
        if (!(coordinates instanceof List<?> cells)) {
            return false;
        }
        return cells.stream().anyMatch(cell -> cell instanceof Map<?, ?> map
                && Boolean.TRUE.equals(map.get("mergeContinuation")));
    }
}
