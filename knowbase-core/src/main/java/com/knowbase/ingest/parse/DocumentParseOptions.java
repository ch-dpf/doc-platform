package com.knowbase.ingest.parse;

/**
 * 单次文档解析选项（来自库级 parsing 规则）。
 */
public record DocumentParseOptions(
        boolean ocrEnabled,
        String language,
        boolean autoDetectEncoding,
        String contentLanguage,
        String contentEncoding,
        TableExtractionMode tableExtraction,
        ImageExtractionMode imageExtraction,
        FormulaExtractionMode formulaExtraction) {

    public DocumentParseOptions {
        if (tableExtraction == null) {
            tableExtraction = TableExtractionMode.TEXT_ONLY;
        }
        if (imageExtraction == null) {
            imageExtraction = ImageExtractionMode.SKIP;
        }
        if (formulaExtraction == null) {
            formulaExtraction = FormulaExtractionMode.SKIP;
        }
        if (contentLanguage == null || contentLanguage.isBlank()) {
            contentLanguage = "zh-CN";
        }
    }

    public static DocumentParseOptions disabled() {
        return new DocumentParseOptions(
                false,
                "chi_sim+eng",
                true,
                "zh-CN",
                null,
                TableExtractionMode.TEXT_ONLY,
                ImageExtractionMode.SKIP,
                FormulaExtractionMode.SKIP);
    }

    public boolean requiresHtmlPipeline() {
        return tableExtraction != TableExtractionMode.TEXT_ONLY
                || imageExtraction != ImageExtractionMode.SKIP
                || formulaExtraction != FormulaExtractionMode.SKIP;
    }
}
