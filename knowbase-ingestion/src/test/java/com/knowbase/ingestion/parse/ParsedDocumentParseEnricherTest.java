package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.domain.status.ContentFamily;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsedDocumentParseEnricherTest {

    @Test
    void enrichesMarkdownBlocksWithUniversalMetadata() {
        ParsedDocument parsed = new ParsedDocument(
                "memory://guide.md",
                "Guide",
                "# Install\n\nBody",
                ContentFamily.RICH_TEXT,
                Map.of("parserCode", "markdown-structure"),
                List.of(
                        StructuralBlock.heading(1, "Install", 0),
                        StructuralBlock.paragraph("Body", 1)
                )
        );

        ParsedDocument enriched = ParsedDocumentParseEnricher.enrich(parsed);

        assertFalse(Boolean.TRUE.equals(enriched.blocks().getFirst().metadata().get("indexableHint")));
        assertTrue(Boolean.TRUE.equals(enriched.blocks().get(1).metadata().get("indexableHint")));
        assertNotNull(enriched.metadata().get("parseConfidence"));
        assertEquals(1, ((Number) enriched.metadata().get("indexableBlockCount")).intValue());
        assertEquals("markdown-structure", enriched.metadata().get("parseConfidenceSource"));
    }

    @Test
    void preservesParserSpecificConfidence() {
        ParsedDocument parsed = new ParsedDocument(
                "memory://report.xlsx",
                "report.xlsx",
                "data",
                ContentFamily.STRUCTURED_TABLE,
                Map.of("parseConfidence", 0.91d, "parseConfidenceSource", "table-adaptive", "tableRegionCount", 2),
                List.of(new StructuralBlock("table_row", 0, "x", 0, Map.of("rowRole", "DATA", "indexableHint", true)))
        );

        ParsedDocument enriched = ParsedDocumentParseEnricher.enrich(parsed);

        assertEquals(0.91d, ((Number) enriched.metadata().get("parseConfidence")).doubleValue(), 0.001d);
        assertEquals("table-adaptive", enriched.metadata().get("parseConfidenceSource"));
        assertEquals(2, enriched.metadata().get("tableRegionCount"));
    }

    @Test
    void appliesOcrConfidencePolicyForOcrBlocks() {
        ParsedDocument parsed = new ParsedDocument(
                "memory://scan.png",
                "scan.png",
                "text",
                ContentFamily.IMAGE_TEXT,
                Map.of("parserCode", "ocr-layout", "ocrConfidenceThreshold", 0.7d, "ocrDownweightMode", "filter"),
                List.of(new StructuralBlock(
                        "paragraph",
                        0,
                        "blurry",
                        0,
                        Map.of("ocrApplied", true, "ocrConfidence", 0.4d, "bbox", List.of(1d, 2d, 3d, 4d))
                ))
        );

        ParsedDocument enriched = ParsedDocumentParseEnricher.enrich(parsed);

        assertTrue(Boolean.TRUE.equals(enriched.blocks().getFirst().metadata().get("lowConfidenceOcr")));
        assertEquals(false, enriched.blocks().getFirst().metadata().get("indexableHint"));
    }
}
