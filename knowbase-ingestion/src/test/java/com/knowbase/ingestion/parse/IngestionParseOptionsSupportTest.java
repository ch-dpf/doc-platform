package com.knowbase.ingestion.parse;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.status.ContentFamily;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IngestionParseOptionsSupportTest {

    @Test
    void mergesProfileOptionsOverDefaults() {
        DocumentProfile profile = new DocumentProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default_scanned_document",
                ContentFamily.SCANNED_DOCUMENT,
                "ocr-layout",
                "page_token_window",
                null,
                Map.of(),
                Map.of(
                        "ocrEngine", "paddle",
                        "ocrDownweightMode", "review",
                        "ocrConfidenceThreshold", 0.75d
                ),
                true
        );
        Map<String, Object> merged = IngestionParseOptionsSupport.mergeForLoad(
                profile,
                Map.of("ocrLanguage", "eng")
        );
        IngestionParseOptionsSupport.IngestionParseOptions options = IngestionParseOptionsSupport.resolve(merged);
        assertEquals("paddle", options.ocrEngine());
        assertEquals("eng", options.ocrLanguage());
        assertEquals(0.75d, options.ocrConfidenceThreshold(), 0.001d);
        assertEquals(OcrDownweightMode.REVIEW, options.ocrDownweightMode());
    }
}
