package com.knowbase.ingestion;

import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import com.knowbase.ingestion.testsupport.IngestionEvalFixtureFactory;
import com.knowbase.ingestion.testsupport.ParserOutputSnapshot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleXlsxParseRegressionTest {

    @Test
    void xlsxProducesAdaptiveTableRowsWithSheetMetadata() throws Exception {
        ParsedDocument parsed = parseXlsxFixture(IngestionEvalFixtureFactory.XLSX_METRICS);

        assertTrue(parsed.structureAware());
        assertNotNull(parsed.metadata().get("parseConfidence"));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "DATA".equals(block.metadata().get("rowRole"))));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "张三".equals(block.content()) || block.content().contains("张三")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "Metrics".equals(block.metadata().get("sheetName"))));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("evidenceAssetHint")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_summary".equals(block.blockType())));
        assertEquals("table-deep", parsed.metadata().get("parser"));

        ParserOutputSnapshot.Signature signature = ParserOutputSnapshot.capture(parsed);
        assertEquals(3, signature.blockCount());
        assertTrue(signature.rowRoles().stream().anyMatch("DATA"::equals));
    }

    @Test
    void xlsxMultiHeaderProducesHeaderPathOnDataRows() throws Exception {
        ParsedDocument parsed = parseXlsxFixture(IngestionEvalFixtureFactory.XLSX_MULTI_HEADER);
        assertTrue(parsed.blocks().stream().anyMatch(block -> "DATA".equals(block.metadata().get("rowRole"))));
        assertTrue(parsed.blocks().stream()
                .filter(block -> "DATA".equals(block.metadata().get("rowRole")))
                .anyMatch(block -> {
                    Object headerPath = block.metadata().get("headerPath");
                    return headerPath instanceof List<?> paths && paths.size() >= 2;
                }));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.content().contains("张三")));
        assertEquals(4, ParserOutputSnapshot.capture(parsed).blockCount());
    }

    private static ParsedDocument parseXlsxFixture(String fixtureId) {
        return ParsedDocumentParseEnricher.enrich(new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://" + IngestionEvalFixtureFactory.filename(fixtureId),
                IngestionEvalFixtureFactory.filename(fixtureId),
                IngestionEvalFixtureFactory.mimeType(fixtureId),
                new ByteArrayInputStream(IngestionEvalFixtureFactory.bytes(fixtureId)),
                Map.of()
        )));
    }
}
