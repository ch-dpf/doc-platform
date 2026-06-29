package com.knowbase.ingestion;

import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import com.knowbase.ingestion.testsupport.IngestionEvalFixtureFactory;
import com.knowbase.ingestion.testsupport.ParserOutputSnapshot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SamplePdfParseRegressionTest {

    @Test
    void pdfTableProducesLayoutBlocksWithCitationMetadata() throws Exception {
        ParsedDocument parsed = parsePdfFixture(IngestionEvalFixtureFactory.PDF_TABLE);

        assertTrue(parsed.structureAware());
        assertNotNull(parsed.metadata().get("parseConfidence"));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("pageNumber")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_row".equals(block.blockType())));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.content().contains("Bob")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("evidenceAssetHint")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_summary".equals(block.blockType())));
        assertTrue(parsed.blocks().stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .anyMatch(block -> block.metadata().containsKey("tableGrid")));
        assertTrue(parsed.blocks().stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .anyMatch(block -> block.metadata().containsKey("cellCoordinates")));
        assertTrue(parsed.metadata().containsKey("pageWidths"));
        assertTrue(parsed.metadata().containsKey("pageHeights"));

        ParserOutputSnapshot.Signature signature = ParserOutputSnapshot.capture(parsed);
        assertEquals(4, signature.blockCount());
        assertTrue(signature.tableRegionIds().stream().distinct().count() >= 1);
        assertNotNull(signature.parseConfidence());
    }

    @Test
    void pdfMultiColumnPreservesReadingOrderMetadata() throws Exception {
        ParsedDocument parsed = parsePdfFixture(IngestionEvalFixtureFactory.PDF_MULTI_COLUMN);
        assertEquals(4, parsed.blocks().size());
        assertTrue(parsed.blocks().stream().anyMatch(block -> Boolean.TRUE.equals(block.metadata().get("multiColumn"))));
        assertTrue(parsed.blocks().stream().allMatch(block -> block.metadata().containsKey("readingOrder")));
    }

    @Test
    void pdfFormulaLineProducesFormulaBlock() throws Exception {
        ParsedDocument parsed = parsePdfFixture(IngestionEvalFixtureFactory.PDF_FORMULA);
        assertTrue(parsed.blocks().stream().anyMatch(block -> "formula".equals(block.blockType())));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("formulaLatex")));
    }

    private static ParsedDocument parsePdfFixture(String fixtureId) {
        return ParsedDocumentParseEnricher.enrich(new PdfLayoutParser().parse(new DocumentSource(
                "memory://" + IngestionEvalFixtureFactory.filename(fixtureId),
                IngestionEvalFixtureFactory.filename(fixtureId),
                IngestionEvalFixtureFactory.mimeType(fixtureId),
                new ByteArrayInputStream(IngestionEvalFixtureFactory.bytes(fixtureId)),
                IngestionEvalFixtureFactory.metadata(fixtureId)
        )));
    }
}
