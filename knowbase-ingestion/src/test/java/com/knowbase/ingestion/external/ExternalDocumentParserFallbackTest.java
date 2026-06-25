package com.knowbase.ingestion.external;

import com.knowbase.ingestion.DocumentSource;
import com.knowbase.ingestion.ExternalDocumentParser;
import com.knowbase.ingestion.PdfLayoutParser;
import com.knowbase.ingestion.ParsedDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalDocumentParserFallbackTest {

    @Test
    void fallsBackToBuiltInPdfParserWhenHttpFails() {
        ExternalDocumentParser parser = new ExternalDocumentParser(
                HttpClient.newHttpClient(),
                "http://127.0.0.1:1/unreachable",
                List.of(new PdfLayoutParser())
        );
        byte[] minimalPdf = minimalPdfBytes();
        ParsedDocument parsed = parser.parse(new DocumentSource(
                "memory://fallback.pdf",
                "fallback.pdf",
                "application/pdf",
                new ByteArrayInputStream(minimalPdf),
                Map.of("externalParserFallback", true)
        ));
        assertNotNull(parsed);
        assertTrue(Boolean.TRUE.equals(parsed.metadata().get("externalParserFallback"))
                || parsed.metadata().containsKey("layoutParsing"));
    }

    private static byte[] minimalPdfBytes() {
        try {
            org.apache.pdfbox.pdmodel.PDDocument document = new org.apache.pdfbox.pdmodel.PDDocument();
            document.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            document.save(outputStream);
            document.close();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
