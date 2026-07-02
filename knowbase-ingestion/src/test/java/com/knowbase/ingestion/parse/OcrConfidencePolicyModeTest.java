package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.ocr.OcrConfidencePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrConfidencePolicyModeTest {

    @Test
    void filterModeMarksBlockNonIndexable() {
        List<StructuralBlock> blocks = OcrConfidencePolicy.apply(
                List.of(blockWithConfidence(0.3d)),
                0.6d,
                OcrDownweightMode.FILTER
        );
        assertFalse(Boolean.TRUE.equals(blocks.getFirst().metadata().get("indexableHint")));
        assertTrue(Boolean.TRUE.equals(blocks.getFirst().metadata().get("lowConfidenceOcr")));
    }

    @Test
    void downweightModeKeepsIndexableWithFactor() {
        List<StructuralBlock> blocks = OcrConfidencePolicy.apply(
                List.of(blockWithConfidence(0.3d)),
                0.6d,
                OcrDownweightMode.DOWNWEIGHT
        );
        assertTrue(blocks.getFirst().metadata().get("indexableHint") == null
                || Boolean.TRUE.equals(blocks.getFirst().metadata().get("indexableHint")));
        assertEquals(0.5d, ((Number) blocks.getFirst().metadata().get("ocrDownweightFactor")).doubleValue(), 0.001d);
    }

    @Test
    void reviewModeFlagsReviewRequired() {
        List<StructuralBlock> blocks = OcrConfidencePolicy.apply(
                List.of(blockWithConfidence(0.3d)),
                0.6d,
                OcrDownweightMode.REVIEW
        );
        assertTrue(Boolean.TRUE.equals(blocks.getFirst().metadata().get("reviewRequired")));
    }

    private static StructuralBlock blockWithConfidence(double confidence) {
        return new StructuralBlock(
                "paragraph",
                0,
                "text",
                0,
                Map.of("ocrApplied", true, "ocrConfidence", confidence)
        );
    }
}
