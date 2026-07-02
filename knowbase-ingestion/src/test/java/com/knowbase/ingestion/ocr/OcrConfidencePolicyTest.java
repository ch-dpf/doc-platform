package com.knowbase.ingestion.ocr;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.parse.OcrDownweightMode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrConfidencePolicyTest {

    @Test
    void filtersLowConfidenceBlocks() {
        StructuralBlock block = new StructuralBlock(
                "paragraph",
                0,
                "text",
                0,
                Map.of("ocrConfidence", 0.4d)
        );
        OcrConfidencePolicy.Decision decision = OcrConfidencePolicy.evaluate(block, 0.6d, OcrDownweightMode.FILTER);
        assertFalse(decision.indexable());
    }

    @Test
    void downweightsByDefaultMode() {
        StructuralBlock block = new StructuralBlock(
                "paragraph",
                0,
                "text",
                0,
                Map.of("ocrConfidence", 0.4d)
        );
        assertTrue(OcrConfidencePolicy.evaluate(block, 0.6d).indexable());
    }

    @Test
    void keepsUnavailableConfidenceBlocks() {
        StructuralBlock block = new StructuralBlock(
                "paragraph",
                0,
                "text",
                0,
                Map.of("ocrConfidence", -1d)
        );
        assertTrue(OcrConfidencePolicy.evaluate(block, 0.6d).indexable());
    }
}
