package com.knowbase.ingestion;

import com.knowbase.ingestion.testsupport.ParserOutputSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserOutputSnapshotTest {

    @Test
    void csvParseProducesConfidenceSnapshot() {
        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://sales.csv",
                "sales.csv",
                "text/csv",
                new java.io.ByteArrayInputStream("""
                        Region,Q1,Q2
                        APAC,10,12
                        """.getBytes(StandardCharsets.UTF_8)),
                Map.of()
        ));
        ParserOutputSnapshot.Signature signature = ParserOutputSnapshot.capture(parsed);
        assertTrue(signature.blockCount() >= 2);
        assertNotNull(signature.parseConfidence());
    }
}
