package com.knowbase.ingestion.pdf;

import com.knowbase.ingestion.DocumentSource;
import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.layout.LayoutAnalysisService;
import com.knowbase.ingestion.layout.OcrRasterLayoutProvider;
import com.knowbase.ingestion.layout.VisionMarkdownLayoutProvider;
import com.knowbase.model.vision.VisionDocumentModelClient;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfVisionDocumentRouterTest {

    @Test
    void routesScannedPdfWhenVisionConfigured() {
        var analysis = new PdfTextExtractabilityAnalyzer.Analysis(1, 0, 0d, true, false);
        var settings = settingsWithStubClient();
        assertTrue(PdfVisionDocumentRouter.shouldRouteToVision(analysis, Map.of(), false, null, settings));
    }

    @Test
    void skipsWhenVisionModelNotConfigured() {
        var analysis = new PdfTextExtractabilityAnalyzer.Analysis(1, 0, 0d, true, false);
        assertFalse(PdfVisionDocumentRouter.shouldRouteToVision(
                analysis,
                Map.of(),
                false,
                null,
                VisionDocumentParseSettings.disabled()
        ));
    }

    @Test
    void honorsExplicitVisionParseMode() {
        var analysis = new PdfTextExtractabilityAnalyzer.Analysis(2, 500, 250d, false, false);
        var settings = settingsWithStubClient();
        assertTrue(PdfVisionDocumentRouter.shouldRouteToVision(
                analysis,
                Map.of("pdfParseMode", "paddleocr-vl"),
                false,
                null,
                settings
        ));
    }

    @Test
    void routesLowConfidenceLayoutToVision() {
        var analysis = new PdfTextExtractabilityAnalyzer.Analysis(1, 120, 120d, false, false);
        var confidence = new PdfParseConfidenceAggregator.PdfParseConfidence(0.4d, java.util.List.of("low_bbox_coverage"), 0, 0, 0);
        var settings = new VisionDocumentParseSettings(settingsWithStubClient().layoutAnalysisService(), false, true, 0.55d, true, 0);
        assertTrue(PdfVisionDocumentRouter.shouldRouteToVision(analysis, Map.of(), false, confidence, settings));
    }

    @Test
    void parsesSamplePdfWithStubVisionClient() throws Exception {
        Path pdf = Path.of("sample-documents/pdf/simple-table.pdf");
        if (!Files.exists(pdf)) {
            return;
        }
        byte[] bytes = Files.readAllBytes(pdf);
        var settings = new VisionDocumentParseSettings(settingsWithStubClient().layoutAnalysisService(), true, true, 0.55d, true, 2);
        ParsedDocument parsed = PdfVisionDocumentRouter.parseWithVision(
                new DocumentSource(
                        pdf.toUri().toString(),
                        pdf.getFileName().toString(),
                        "application/pdf",
                        new ByteArrayInputStream(bytes),
                        Map.of()
                ),
                bytes,
                PdfTextExtractabilityAnalyzer.analyze(bytes),
                settings
        );
        assertEquals("vision-vl", parsed.metadata().get("pdfParseRoute"));
        assertTrue(parsed.blocks().size() >= 1);
        assertEquals("stub-vl", parsed.metadata().get("visionLanguageModel"));
    }

    private static VisionDocumentParseSettings settingsWithStubClient() {
        LayoutAnalysisService service = new LayoutAnalysisService(
                List.of(new VisionMarkdownLayoutProvider(new StubVisionClient()), new OcrRasterLayoutProvider()),
                true
        );
        return new VisionDocumentParseSettings(service, true, true, 0.55d, true, 0);
    }

    private static final class StubVisionClient implements VisionDocumentModelClient {
        @Override
        public String modelName() {
            return "stub-vl";
        }

        @Override
        public String recognizePage(byte[] imageBytes, String mimeType, int pageNumber, Map<String, Object> options) {
            return "# Page " + pageNumber + "\n\nStub vision content for page " + pageNumber;
        }
    }
}
