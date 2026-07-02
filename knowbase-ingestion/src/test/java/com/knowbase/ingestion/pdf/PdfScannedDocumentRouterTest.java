package com.knowbase.ingestion.pdf;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfScannedDocumentRouterTest {

    @Test
    void routesBlankPdfToOcr() {
        var analysis = new PdfTextExtractabilityAnalyzer.Analysis(1, 0, 0d, true, false);
        assertTrue(PdfScannedDocumentRouter.shouldRouteToOcr(analysis, Map.of(), true));
    }

    @Test
    void respectsPdfOcrFallbackDisableFlag() {
        var analysis = new PdfTextExtractabilityAnalyzer.Analysis(1, 0, 0d, true, false);
        assertFalse(PdfScannedDocumentRouter.shouldRouteToOcr(analysis, Map.of("pdfOcrFallback", false), false));
    }

    @Test
    void honorsExplicitOcrParseMode() {
        var analysis = new PdfTextExtractabilityAnalyzer.Analysis(2, 500, 250d, false, false);
        assertTrue(PdfScannedDocumentRouter.shouldRouteToOcr(analysis, Map.of("pdfParseMode", "ocr"), false));
    }
}
