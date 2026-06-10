package com.knowbase.ingest.parse;

/**
 * @deprecated 使用 {@link HtmlParsingContentProcessor}。
 */
@Deprecated
public final class HtmlTableExtractionProcessor {

    private HtmlTableExtractionProcessor() {}

    public static String apply(String html, TableExtractionMode mode) {
        DocumentParseOptions options = new DocumentParseOptions(
                false,
                "chi_sim+eng",
                true,
                "zh-CN",
                null,
                mode,
                ImageExtractionMode.SKIP,
                FormulaExtractionMode.SKIP);
        return HtmlParsingContentProcessor.apply(html, options);
    }
}
