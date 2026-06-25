package com.knowbase.ingestion.parse;

import java.util.Locale;
import java.util.Map;

public enum OcrDownweightMode {
    FILTER,
    DOWNWEIGHT,
    REVIEW;

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static OcrDownweightMode from(Object raw) {
        if (raw == null) {
            return DOWNWEIGHT;
        }
        String normalized = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "filter", "exclude", "drop" -> FILTER;
            case "review", "flag" -> REVIEW;
            default -> DOWNWEIGHT;
        };
    }

    public static OcrDownweightMode from(Map<String, Object> options) {
        if (options == null) {
            return DOWNWEIGHT;
        }
        return from(options.get("ocrDownweightMode"));
    }
}
