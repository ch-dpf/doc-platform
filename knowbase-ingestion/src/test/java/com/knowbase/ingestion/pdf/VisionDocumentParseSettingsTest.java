package com.knowbase.ingestion.pdf;

import com.knowbase.ingestion.layout.LayoutAnalysisService;
import com.knowbase.ingestion.layout.OcrRasterLayoutProvider;
import com.knowbase.ingestion.layout.VisionMarkdownLayoutProvider;
import com.knowbase.model.vision.VisionDocumentModelClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisionDocumentParseSettingsTest {

    @Test
    void disabledWhenServiceMissing() {
        assertFalse(VisionDocumentParseSettings.disabled().available());
    }

    @Test
    void availableWhenLayoutServiceConfigured() {
        VisionDocumentModelClient client = new VisionDocumentModelClient() {
            @Override
            public String modelName() {
                return "test-vl";
            }

            @Override
            public String recognizePage(byte[] imageBytes, String mimeType, int pageNumber, java.util.Map<String, Object> options) {
                return "text";
            }
        };
        LayoutAnalysisService service = new LayoutAnalysisService(
                List.of(new VisionMarkdownLayoutProvider(client), new OcrRasterLayoutProvider()),
                true
        );
        var settings = new VisionDocumentParseSettings(service, true, true, 0.55d, true, 0);
        assertTrue(settings.available());
    }
}
