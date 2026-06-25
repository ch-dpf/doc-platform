package com.knowbase.ingestion.pdf;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfParseConfidenceAggregatorTest {

    @Test
    void rewardsBlocksWithBbox() {
        PdfParseConfidenceAggregator.PdfParseConfidence confidence = PdfParseConfidenceAggregator.aggregate(List.of(
                block("paragraph", Map.of("bbox", List.of(1, 2, 3, 4), "layoutRole", "body")),
                block("paragraph", Map.of("bbox", List.of(1, 2, 3, 4), "layoutRole", "body"))
        ));
        assertTrue(confidence.score() >= 0.8d);
        assertTrue(confidence.reasons().isEmpty());
    }

    @Test
    void penalizesMissingBboxCoverage() {
        PdfParseConfidenceAggregator.PdfParseConfidence confidence = PdfParseConfidenceAggregator.aggregate(List.of(
                block("paragraph", Map.of("layoutRole", "body")),
                block("paragraph", Map.of())
        ));
        assertFalse(confidence.reasons().isEmpty());
    }

    private static StructuralBlock block(String type, Map<String, Object> metadata) {
        return new StructuralBlock(type, 0, "text", 0, metadata);
    }
}
