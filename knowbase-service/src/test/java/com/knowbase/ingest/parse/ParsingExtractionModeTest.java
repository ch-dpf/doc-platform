package com.knowbase.ingest.parse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsingExtractionModeTest {

    @Test
    void mapsImageExtractionValues() {
        assertTrue(ImageExtractionMode.fromConfig("ocr-caption") == ImageExtractionMode.OCR_CAPTION);
        assertTrue(ImageExtractionMode.fromConfig("skip") == ImageExtractionMode.SKIP);
        assertTrue(ImageExtractionMode.fromConfig(null) == ImageExtractionMode.SKIP);
    }

    @Test
    void mapsFormulaExtractionValues() {
        assertTrue(FormulaExtractionMode.fromConfig("latex") == FormulaExtractionMode.LATEX);
        assertTrue(FormulaExtractionMode.fromConfig("skip") == FormulaExtractionMode.SKIP);
        assertTrue(FormulaExtractionMode.fromConfig(null) == FormulaExtractionMode.SKIP);
    }

    @Test
    void htmlPipelineRequiredWhenAnyRuleIsActive() {
        assertTrue(new DocumentParseOptions(
                        false,
                        "chi_sim+eng",
                        true,
                        "zh-CN",
                        null,
                        TableExtractionMode.STRUCTURED,
                        ImageExtractionMode.SKIP,
                        FormulaExtractionMode.SKIP)
                .requiresHtmlPipeline());
        assertTrue(new DocumentParseOptions(
                        false,
                        "chi_sim+eng",
                        true,
                        "zh-CN",
                        null,
                        TableExtractionMode.TEXT_ONLY,
                        ImageExtractionMode.OCR_CAPTION,
                        FormulaExtractionMode.SKIP)
                .requiresHtmlPipeline());
        assertTrue(new DocumentParseOptions(
                        false,
                        "chi_sim+eng",
                        true,
                        "zh-CN",
                        null,
                        TableExtractionMode.TEXT_ONLY,
                        ImageExtractionMode.SKIP,
                        FormulaExtractionMode.LATEX)
                .requiresHtmlPipeline());
        assertFalse(DocumentParseOptions.disabled().requiresHtmlPipeline());
    }
}
