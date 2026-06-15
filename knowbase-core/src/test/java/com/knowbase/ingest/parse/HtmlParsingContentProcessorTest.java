package com.knowbase.ingest.parse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlParsingContentProcessorTest {

    private static final String SAMPLE_HTML =
            """
            <html><body>
            <p>Intro paragraph</p>
            <table>
              <tr><th>Name</th><th>Score</th></tr>
              <tr><td>Alice</td><td>95</td></tr>
            </table>
            <img alt="架构图" src="diagram.png" />
            <math><annotation encoding="application/x-tex">E=mc^2</annotation></math>
            <p>Outro paragraph</p>
            </body></html>
            """;

    private static DocumentParseOptions options(
            TableExtractionMode table, ImageExtractionMode image, FormulaExtractionMode formula) {
        return new DocumentParseOptions(
                false, "chi_sim+eng", true, "zh-CN", null, table, image, formula);
    }

    @Test
    void structuredModeConvertsTablesToMarkdown() {
        String result = HtmlParsingContentProcessor.apply(
                SAMPLE_HTML, options(TableExtractionMode.STRUCTURED, ImageExtractionMode.SKIP, FormulaExtractionMode.SKIP));

        assertTrue(result.contains("| Name | Score |"));
        assertTrue(result.contains("| Alice | 95 |"));
    }

    @Test
    void skipModeRemovesTablesButKeepsSurroundingText() {
        String result = HtmlParsingContentProcessor.apply(
                SAMPLE_HTML, options(TableExtractionMode.SKIP, ImageExtractionMode.SKIP, FormulaExtractionMode.SKIP));

        assertTrue(result.contains("Intro paragraph"));
        assertFalse(result.contains("Alice"));
    }

    @Test
    void imageSkipRemovesImageTags() {
        String result = HtmlParsingContentProcessor.apply(
                SAMPLE_HTML, options(TableExtractionMode.TEXT_ONLY, ImageExtractionMode.SKIP, FormulaExtractionMode.SKIP));

        assertFalse(result.contains("架构图"));
        assertFalse(result.contains("[图片"));
    }

    @Test
    void imageOcrCaptionUsesAltText() {
        String result = HtmlParsingContentProcessor.apply(
                SAMPLE_HTML,
                options(TableExtractionMode.TEXT_ONLY, ImageExtractionMode.OCR_CAPTION, FormulaExtractionMode.SKIP));

        assertTrue(result.contains("[图片: 架构图]"));
    }

    @Test
    void imageOcrCaptionPrefersEmbeddedOcrResult() {
        String html =
                "<img alt=\"fallback\" src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==\" />";
        String result = HtmlParsingContentProcessor.apply(
                html,
                options(TableExtractionMode.TEXT_ONLY, ImageExtractionMode.OCR_CAPTION, FormulaExtractionMode.SKIP),
                (bytes, mime) -> "识别文字");

        assertTrue(result.contains("[图片: 识别文字]"));
        assertFalse(result.contains("fallback"));
    }

    @Test
    void formulaSkipRemovesMathBlocks() {
        String result = HtmlParsingContentProcessor.apply(
                SAMPLE_HTML, options(TableExtractionMode.TEXT_ONLY, ImageExtractionMode.SKIP, FormulaExtractionMode.SKIP));

        assertFalse(result.contains("E=mc^2"));
        assertFalse(result.contains("$"));
    }

    @Test
    void formulaLatexUsesAnnotation() {
        String result = HtmlParsingContentProcessor.apply(
                SAMPLE_HTML,
                options(TableExtractionMode.TEXT_ONLY, ImageExtractionMode.SKIP, FormulaExtractionMode.LATEX));

        assertTrue(result.contains("$E=mc^2$"));
    }

    @Test
    void blankHtmlReturnsEmptyString() {
        assertEquals("", HtmlParsingContentProcessor.apply("", DocumentParseOptions.disabled()));
    }
}
