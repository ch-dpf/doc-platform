package com.knowbase.ingestion;

import com.knowbase.ingestion.ocr.OcrBlockFactory;
import com.knowbase.ingestion.ocr.OcrConfidencePolicy;
import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleOcrHocrRegressionTest {

    @Test
    void hocrProducesBlocksWithConfidenceBboxAndLowConfidenceFlags() throws Exception {
        byte[] bytes = SampleOcrHocrRegressionTest.class.getClassLoader()
                .getResourceAsStream("sample-documents/ocr/sample-scan.hocr")
                .readAllBytes();
        String hocr = new String(bytes, StandardCharsets.UTF_8);
        var blocks = OcrConfidencePolicy.apply(
                OcrBlockFactory.fromHocr(hocr, Map.of("ocrLanguage", "chi_sim", "pageNumber", 1)),
                0.6d
        );
        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(new ParsedDocument(
                "memory://sample-scan.hocr",
                "sample-scan.hocr",
                "",
                com.knowbase.domain.status.ContentFamily.IMAGE_TEXT,
                Map.of("parserCode", "ocr-layout"),
                blocks
        ));

        assertTrue(parsed.blocks().stream().allMatch(block -> block.metadata().containsKey("bbox")));
        assertTrue(parsed.blocks().stream().allMatch(block -> block.metadata().containsKey("ocrConfidence")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "chi_sim".equals(block.metadata().get("ocrLanguage"))));
        assertTrue(parsed.blocks().stream().anyMatch(block -> Boolean.TRUE.equals(block.metadata().get("lowConfidenceOcr"))));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("evidenceAssetHint")));
        assertNotNull(parsed.metadata().get("parseConfidence"));
    }
}
