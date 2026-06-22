package com.knowbase.ingestion;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParseOptionsSupportTest {

    @Test
    void resolvesLayoutParserForPdf() {
        Map<String, Object> options = Map.of("parseMode", "layout");
        Map<String, Object> routed = ParseOptionsSupport.applyParseMode(options, "minio://knowbase/docs/report.pdf");
        assertEquals(PdfLayoutParser.PARSER_CODE, routed.get("parserCode"));
        assertTrue(ParseOptionsSupport.isLayoutMode(routed));
    }

    @Test
    void resolvesOcrParserForImage() {
        Map<String, Object> options = Map.of("enableOcr", true, "ocrLanguage", "chi_sim");
        Map<String, Object> routed = ParseOptionsSupport.applyParseMode(options, "minio://knowbase/scans/page.png");
        assertEquals(OcrLayoutDocumentParser.PARSER_CODE, routed.get("parserCode"));
        assertTrue(ParseOptionsSupport.isOcrMode(routed));
    }
}
