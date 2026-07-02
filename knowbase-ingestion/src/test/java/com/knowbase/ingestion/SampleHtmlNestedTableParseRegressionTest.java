package com.knowbase.ingestion;

import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleHtmlNestedTableParseRegressionTest {

    @Test
    void nestedAndFloatingTablesReceiveIndependentRegions() throws Exception {
        byte[] bytes = SampleHtmlNestedTableParseRegressionTest.class.getClassLoader()
                .getResourceAsStream("sample-documents/html/nested-table.html")
                .readAllBytes();

        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(new HtmlStructureParser().parse(new DocumentSource(
                "memory://nested-table.html",
                "nested-table.html",
                "text/html",
                new ByteArrayInputStream(bytes),
                Map.of()
        )));

        Set<Object> regionIds = parsed.blocks().stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .map(block -> block.metadata().get("tableRegionId"))
                .collect(Collectors.toSet());
        assertEquals(3, regionIds.size());
        assertTrue(parsed.blocks().stream().anyMatch(block ->
                Boolean.TRUE.equals(block.metadata().get("nestedTable"))));
        assertTrue(parsed.blocks().stream().anyMatch(block ->
                Integer.valueOf(0).equals(block.metadata().get("parentTableRegionId"))));
        assertTrue(parsed.blocks().stream().anyMatch(block ->
                "floating".equals(block.metadata().get("tableLayout"))));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.content().contains("HW-01")));
    }
}
