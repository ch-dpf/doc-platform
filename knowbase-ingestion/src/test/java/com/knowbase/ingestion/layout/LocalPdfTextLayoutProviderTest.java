package com.knowbase.ingestion.layout;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalPdfTextLayoutProviderTest {

    @Test
    void extractsTextPositionsFromPdfPage() throws Exception {
        byte[] pdfBytes = buildPdf();
        LocalPdfTextLayoutProvider provider = new LocalPdfTextLayoutProvider();
        Map<String, Object> options = new HashMap<>();
        options.put(LayoutAnalysisOptions.PDF_BYTES, pdfBytes);
        LayoutPageRequest request = new LayoutPageRequest(
                new byte[0],
                "application/pdf",
                1,
                612,
                792,
                "file://sample.pdf",
                options
        );
        assertTrue(provider.supports(request));
        LayoutPageResult result = provider.analyze(request);
        assertFalse(result.blocks().isEmpty());
        assertTrue(result.blocks().stream().anyMatch(block -> block.content().contains("Local layout")));
        assertTrue(result.blocks().stream().allMatch(block ->
                LocalPdfTextLayoutProvider.PROVIDER_CODE.equals(block.metadata().get("layoutProvider"))));
    }

    private static byte[] buildPdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(72, 720);
                stream.showText("Local layout model page");
                stream.endText();
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
