package com.knowbase.ingestion.ocr;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaddleOcrHttpEngineAdapterTest {

    @Test
    void requiresConfiguredEndpoint() {
        PaddleOcrHttpEngineAdapter adapter = new PaddleOcrHttpEngineAdapter();
        assertEquals("paddle", adapter.engineCode());
        assertFalse(adapter.supports("image/png", Map.of()));
        assertTrue(adapter.supports("image/png", Map.of("paddleOcrEndpoint", "http://127.0.0.1:8866/ocr")));
    }
}
