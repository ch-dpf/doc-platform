package com.knowbase.ingest.parse;

/**
 * 库级 parsing.formulaExtraction 取值。
 */
public enum FormulaExtractionMode {

    SKIP("skip"),
    LATEX("latex");

    private final String configValue;

    FormulaExtractionMode(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return configValue;
    }

    public static FormulaExtractionMode fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return SKIP;
        }
        return "latex".equalsIgnoreCase(value.trim()) ? LATEX : SKIP;
    }
}
