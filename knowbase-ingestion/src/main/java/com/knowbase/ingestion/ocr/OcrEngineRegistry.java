package com.knowbase.ingestion.ocr;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves OCR engine adapters from prepare/parse options.
 */
public final class OcrEngineRegistry {

    private static final String DEFAULT_ENGINE = "tesseract";

    private OcrEngineRegistry() {
    }

    public static OcrEngineAdapter resolve(Map<String, Object> options) {
        String requested = readEngineCode(options);
        if ("paddle".equalsIgnoreCase(requested)) {
            OcrEngineAdapter paddle = new PaddleOcrHttpEngineAdapter();
            if (!paddle.supports(null, options)) {
                throw new IllegalStateException(
                        "Paddle OCR 未配置 endpoint（paddleOcrEndpoint / KNOWBASE_PADDLE_OCR_ENDPOINT）");
            }
            return paddle;
        }
        for (OcrEngineAdapter adapter : builtInAdapters()) {
            if (adapter.engineCode().equalsIgnoreCase(requested)) {
                return adapter;
            }
        }
        throw new IllegalStateException("Unsupported OCR engine: " + requested);
    }

    public static List<String> supportedEngineCodes() {
        return builtInAdapters().stream().map(OcrEngineAdapter::engineCode).toList();
    }

    private static String readEngineCode(Map<String, Object> options) {
        if (options == null) {
            return DEFAULT_ENGINE;
        }
        Object raw = options.get("ocrEngine");
        if (raw == null) {
            raw = options.get("ocrEngineCode");
        }
        if (raw == null || String.valueOf(raw).isBlank()) {
            return DEFAULT_ENGINE;
        }
        return String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
    }

    private static List<OcrEngineAdapter> builtInAdapters() {
        return List.of(new TesseractOcrEngineAdapter(), new PaddleOcrHttpEngineAdapter());
    }
}
