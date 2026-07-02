package com.knowbase.ingestion.layout;

import java.util.Map;

public record LayoutPageRequest(
        byte[] imageBytes,
        String mimeType,
        int pageNumber,
        double pageWidth,
        double pageHeight,
        String sourceUri,
        Map<String, Object> options
) {
    public Map<String, Object> effectiveOptions() {
        return options == null ? Map.of() : options;
    }
}
