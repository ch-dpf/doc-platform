package com.knowbase.model.vision;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisionDocumentPromptsTest {

    @Test
    void usesOcrPromptForPaddleOcrVlModel() {
        assertEquals(
                VisionDocumentPrompts.PADDLEOCR_VL_OCR_PROMPT,
                VisionDocumentPrompts.resolvePagePrompt(Map.of(), "PaddleOCR-VL-1.6-0.9B")
        );
    }

    @Test
    void usesTablePromptWhenRequested() {
        assertEquals(
                VisionDocumentPrompts.PADDLEOCR_VL_TABLE_PROMPT,
                VisionDocumentPrompts.resolvePagePrompt(Map.of("vlTask", "table"), "PaddleOCR-VL-1.6-0.9B")
        );
    }

    @Test
    void customPromptOverridesModelDefault() {
        assertEquals(
                "custom",
                VisionDocumentPrompts.resolvePagePrompt(Map.of("vlPrompt", "custom"), "PaddleOCR-VL-1.6-0.9B")
        );
    }

    @Test
    void keepsGenericPromptForNonPaddleModels() {
        assertEquals(
                VisionDocumentPrompts.DEFAULT_PAGE_PROMPT.trim(),
                VisionDocumentPrompts.resolvePagePrompt(Map.of(), "llama3.2-vision").trim()
        );
    }

    @Test
    void detectsPaddleOcrVlModelNames() {
        assertTrue(VisionDocumentPrompts.isPaddleOcrVlModel("PaddleOCR-VL-1.6-0.9B"));
        assertFalse(VisionDocumentPrompts.isPaddleOcrVlModel("llama3.2-vision"));
    }
}
