package com.knowbase.ingestion.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTextExtractabilityAnalyzerTest {

    @Test
    void blankPdfIsScannedLikely() throws Exception {
        byte[] bytes;
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            document.save(outputStream);
            bytes = outputStream.toByteArray();
        }
        PdfTextExtractabilityAnalyzer.Analysis analysis = PdfTextExtractabilityAnalyzer.analyze(bytes);
        assertTrue(analysis.scannedLikely());
        assertTrue(analysis.totalChars() < 24);
    }
}
