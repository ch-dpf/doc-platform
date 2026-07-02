package com.knowbase.ingestion;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleTableParseRegressionTest {

    @Test
    void metricsCsvProducesAdaptiveTableRows() throws Exception {
        byte[] bytes = SampleTableParseRegressionTest.class.getClassLoader()
                .getResourceAsStream("sample-documents/table/metrics.csv")
                .readAllBytes();

        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://metrics.csv",
                "metrics.csv",
                "text/csv",
                new ByteArrayInputStream(bytes),
                Map.of()
        ));

        assertTrue(parsed.structureAware());
        assertNotNull(parsed.metadata().get("parseConfidence"));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "DATA".equals(block.metadata().get("rowRole"))));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.content().contains("张三")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "HEADER".equals(block.metadata().get("rowRole"))));
        assertEquals("table-deep", parsed.metadata().get("parser"));
    }
}
