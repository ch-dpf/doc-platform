package com.knowbase.ingestion;

import java.util.Locale;

public enum PreparationStage {
    PARSE,
    NORMALIZE,
    CHUNK,
    ALL;

    public static PreparationStage from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "parse" -> PARSE;
            case "normalize" -> NORMALIZE;
            case "chunk" -> CHUNK;
            case "all", "full" -> ALL;
            default -> throw new IllegalArgumentException("不支持的 prepareStage: " + value);
        };
    }
}
