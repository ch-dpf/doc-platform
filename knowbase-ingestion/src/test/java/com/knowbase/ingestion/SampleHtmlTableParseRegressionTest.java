package com.knowbase.ingestion;

import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleHtmlTableParseRegressionTest {

    @Test
    void htmlTableProducesAdaptiveTableRows() throws Exception {
        byte[] bytes = SampleHtmlTableParseRegressionTest.class.getClassLoader()
                .getResourceAsStream("sample-documents/html/team-metrics.html")
                .readAllBytes();

        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(new HtmlStructureParser().parse(new DocumentSource(
                "memory://team-metrics.html",
                "team-metrics.html",
                "text/html",
                new ByteArrayInputStream(bytes),
                Map.of()
        )));

        assertTrue(parsed.structureAware());
        assertNotNull(parsed.metadata().get("parseConfidence"));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_row".equals(block.blockType())));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "DATA".equals(block.metadata().get("rowRole"))));
        assertTrue(parsed.blocks().stream().anyMatch(block ->
                "html-table-0".equals(block.metadata().get("tableRegionLabel"))));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_summary".equals(block.blockType())));
    }
}
