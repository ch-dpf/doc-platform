package com.knowbase.ingestion.ocr;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrEngineRegistryTest {

    @Test
    void defaultsToTesseract() {
        OcrEngineAdapter adapter = OcrEngineRegistry.resolve(Map.of());
        assertEquals("tesseract", adapter.engineCode());
    }

    @Test
    void resolvesExplicitEngineCode() {
        OcrEngineAdapter adapter = OcrEngineRegistry.resolve(Map.of("ocrEngine", "tesseract"));
        assertEquals("tesseract", adapter.engineCode());
    }

    @Test
    void rejectsPaddleWithoutEndpoint() {
        assertThrows(IllegalStateException.class, () -> OcrEngineRegistry.resolve(Map.of("ocrEngine", "paddle")));
    }

    @Test
    void resolvesPaddleWithEndpoint() {
        OcrEngineAdapter adapter = OcrEngineRegistry.resolve(Map.of(
                "ocrEngine", "paddle",
                "paddleOcrEndpoint", "http://127.0.0.1:8866/ocr"
        ));
        assertEquals("paddle", adapter.engineCode());
    }

    @Test
    void listsSupportedEngines() {
        assertTrue(OcrEngineRegistry.supportedEngineCodes().contains("tesseract"));
        assertTrue(OcrEngineRegistry.supportedEngineCodes().contains("paddle"));
    }
}
