package com.knowbase.ingest.parse;

/**
 * 库级 parsing.imageExtraction 取值。
 */
public enum ImageExtractionMode {

    SKIP("skip"),
    OCR_CAPTION("ocr-caption");

    private final String configValue;

    ImageExtractionMode(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public static ImageExtractionMode fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return SKIP;
        }
        return "ocr-caption".equalsIgnoreCase(value.trim()) ? OCR_CAPTION : SKIP;
    }
}
