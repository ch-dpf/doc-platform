package com.knowbase.library.config;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "文档解析规则")
public class ParsingRulesSettings {

    @Schema(description = "是否启用 OCR 回退（扫描件/图片）", example = "false")
    private boolean ocrEnabled = false;
    @Schema(description = "表格抽取模式", allowableValues = {"text-only", "structured", "skip"}, example = "text-only")
    private String tableExtraction = "text-only";
    @Schema(description = "图片抽取模式", allowableValues = {"ocr-caption", "skip"}, example = "skip")
    private String imageExtraction = "skip";
    @Schema(description = "公式抽取模式", allowableValues = {"latex", "skip"}, example = "skip")
    private String formulaExtraction = "skip";
    @Schema(description = "是否自动检测文件编码", example = "true")
    private boolean autoDetectEncoding = true;
    @Schema(description = "默认语言（OCR/Tika）", example = "zh-CN")
    private String defaultLanguage = "zh-CN";
    @Schema(description = "是否按 MIME 应用管道默认；null 视为 true", example = "true")
    private Boolean mimeAwareDefaults;

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

    public boolean isMimeAwareDefaults() {
        return mimeAwareDefaults == null || mimeAwareDefaults;
    }

    public Boolean getMimeAwareDefaults() {
        return mimeAwareDefaults;
    }

    public void setMimeAwareDefaults(Boolean mimeAwareDefaults) {
        this.mimeAwareDefaults = mimeAwareDefaults;
    }
}
