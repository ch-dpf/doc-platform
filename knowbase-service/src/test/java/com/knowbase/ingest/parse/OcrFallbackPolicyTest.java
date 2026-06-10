package com.knowbase.ingest.parse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrFallbackPolicyTest {

    @Test
    void fallsBackWhenPdfTextIsSparse() {
        assertTrue(OcrFallbackPolicy.shouldFallback("", "application/pdf", "scan.pdf", 32));
        assertTrue(OcrFallbackPolicy.shouldFallback("short", "application/pdf", "scan.pdf", 32));
    }

    @Test
    void skipsOcrWhenPdfAlreadyHasText() {
        assertFalse(OcrFallbackPolicy.shouldFallback(
                "a".repeat(40), "application/pdf", "digital.pdf", 32));
    }

    @Test
    void skipsOcrForNonEligibleMime() {
        assertFalse(OcrFallbackPolicy.shouldFallback("", "text/plain", "readme.txt", 32));
    }
}
