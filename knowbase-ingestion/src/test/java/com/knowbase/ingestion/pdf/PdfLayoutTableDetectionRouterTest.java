package com.knowbase.ingestion.pdf;

import com.knowbase.ingestion.DocumentSource;
import com.knowbase.ingestion.layout.OllamaLayoutTableProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfLayoutTableDetectionRouterTest {

    @Test
    void usesOllamaWhenLayoutProviderConfigured() {
        assertTrue(PdfLayoutTableDetectionRouter.resolveParseRoute(
                Map.of("layoutProvider", OllamaLayoutTableProvider.PROVIDER_CODE),
                true
        ).contains("ollama"));
    }

    @Test
    void fallsBackToHeuristicRouteWhenMlNotUsed() {
        assertFalse(PdfLayoutTableDetectionRouter.resolveParseRoute(Map.of(), false).contains("ollama"));
    }

    @Test
    void tableMlDetectionFlagEnablesRouting() throws Exception {
        byte[] pdf = SamplePdfBytes.simpleTablePdf();
        DocumentSource source = new DocumentSource(
                "memory://table.pdf",
                "table.pdf",
                "application/pdf",
                new java.io.ByteArrayInputStream(pdf),
                Map.of("tableMlDetection", true, "layoutProvider", OllamaLayoutTableProvider.PROVIDER_CODE)
        );
        assertTrue(PdfLayoutTableDetectionRouter.extractBlocks(source, pdf, null, true).size() >= 1);
    }
}
