package com.knowbase.ingestion;

import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleMultiHeaderTableParseRegressionTest {

    @Test
    void multiHeaderCsvProducesHeaderPathOnDataRows() throws Exception {
        byte[] bytes = SampleMultiHeaderTableParseRegressionTest.class.getClassLoader()
                .getResourceAsStream("sample-documents/table/multi-header-metrics.csv")
                .readAllBytes();

        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://multi-header-metrics.csv",
                "multi-header-metrics.csv",
                "text/csv",
                new ByteArrayInputStream(bytes),
                Map.of()
        )));

        assertTrue(parsed.structureAware());
        assertNotNull(parsed.metadata().get("parseConfidence"));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "DATA".equals(block.metadata().get("rowRole"))));
        assertTrue(parsed.blocks().stream()
                .filter(block -> "DATA".equals(block.metadata().get("rowRole")))
                .anyMatch(block -> {
                    Object headerPath = block.metadata().get("headerPath");
                    return headerPath instanceof List<?> paths && paths.size() >= 2;
                }));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.content().contains("张三")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_summary".equals(block.blockType())));
    }
}
