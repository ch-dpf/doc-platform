package com.knowbase.ingest.parse;

/**
 * 库级 parsing.tableExtraction 取值。
 */
public enum TableExtractionMode {

    TEXT_ONLY("text-only"),
    STRUCTURED("structured"),
    SKIP("skip");

    private final String configValue;

    TableExtractionMode(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public static TableExtractionMode fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return TEXT_ONLY;
        }
        return switch (value.trim().toLowerCase()) {
            case "structured" -> STRUCTURED;
            case "skip" -> SKIP;
            default -> TEXT_ONLY;
        };
    }
}
