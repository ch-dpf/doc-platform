package com.knowbase.ingestion.ocr;

import java.util.Map;

public record OcrRecognizeRequest(String sourceUri, String mimeType, String language, Map<String, Object> options) {
    public OcrRecognizeRequest {
        options = options == null ? Map.of() : Map.copyOf(options);
    }
}
