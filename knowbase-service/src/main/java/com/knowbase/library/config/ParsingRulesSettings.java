package com.knowbase.library.config;

public class ParsingRulesSettings {

    private boolean ocrEnabled = false;
    /** text-only | structured | skip */
    private String tableExtraction = "text-only";
    private String imageExtraction = "skip";
    private String formulaExtraction = "skip";
    private boolean autoDetectEncoding = true;
    private String defaultLanguage = "zh-CN";

    public boolean isOcrEnabled() {
        return ocrEnabled;
    }

    public void setOcrEnabled(boolean ocrEnabled) {
        this.ocrEnabled = ocrEnabled;
    }

    public String getTableExtraction() {
        return tableExtraction;
    }

    public void setTableExtraction(String tableExtraction) {
        this.tableExtraction = tableExtraction;
    }

    public String getImageExtraction() {
        return imageExtraction;
    }

    public void setImageExtraction(String imageExtraction) {
        this.imageExtraction = imageExtraction;
    }

    public String getFormulaExtraction() {
        return formulaExtraction;
    }

    public void setFormulaExtraction(String formulaExtraction) {
        this.formulaExtraction = formulaExtraction;
    }

    public boolean isAutoDetectEncoding() {
        return autoDetectEncoding;
    }

    public void setAutoDetectEncoding(boolean autoDetectEncoding) {
        this.autoDetectEncoding = autoDetectEncoding;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
}
