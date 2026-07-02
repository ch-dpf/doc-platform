package com.knowbase.ingestion.ocr;

import java.util.List;
import java.util.Map;

public record OcrEngineResult(
        String engineCode,
        String rawFormat,
        String rawPayload,
        List<OcrPageResult> pages,
        Map<String, Object> engineMetadata
) {
    public OcrEngineResult {
        pages = pages == null ? List.of() : List.copyOf(pages);
        engineMetadata = engineMetadata == null ? Map.of() : Map.copyOf(engineMetadata);
    }
}
