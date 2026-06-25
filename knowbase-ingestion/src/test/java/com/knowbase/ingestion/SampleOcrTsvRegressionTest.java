package com.knowbase.ingestion;

import com.knowbase.ingestion.ocr.OcrBlockFactory;
import com.knowbase.ingestion.ocr.OcrEngineResult;
import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleOcrTsvRegressionTest {

    @Test
    void tsvProducesWordLevelMetadataAndParseEnrichment() throws Exception {
        byte[] bytes = SampleOcrTsvRegressionTest.class.getClassLoader()
                .getResourceAsStream("sample-documents/ocr/sample-scan.tsv")
                .readAllBytes();
        String tsv = new String(bytes, StandardCharsets.UTF_8);
        var blocks = OcrBlockFactory.fromEngineResult(
                new OcrEngineResult("tesseract-tsv", "tsv", tsv, List.of(), Map.of("ocrApplied", true)),
                Map.of("pageNumber", 1, "ocrLanguage", "eng")
        );
        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(new ParsedDocument(
                "memory://sample-scan.tsv",
                "sample-scan.tsv",
                "",
                com.knowbase.domain.status.ContentFamily.IMAGE_TEXT,
                Map.of("parserCode", "ocr-layout", "ocrConfidenceThreshold", 0.6d),
                blocks
        ));

        assertTrue(parsed.blocks().stream().anyMatch(block -> block.content().contains("Invoice Total")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> {
            Object words = block.metadata().get("ocrWords");
            return words instanceof java.util.List<?> list && list.size() >= 2;
        }));
        assertTrue(parsed.blocks().stream().allMatch(block -> block.metadata().containsKey("ocrConfidence")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("evidenceAssetHint")));
        assertNotNull(parsed.metadata().get("parseConfidence"));
    }
}
