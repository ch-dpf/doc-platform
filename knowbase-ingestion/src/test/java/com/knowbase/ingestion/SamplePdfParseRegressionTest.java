package com.knowbase.ingestion;

import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SamplePdfParseRegressionTest {

    @Test
    void pdfTableProducesLayoutBlocksWithCitationMetadata() throws Exception {
        byte[] pdfBytes = buildTablePdf();
        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(new PdfLayoutParser().parse(new DocumentSource(
                "memory://metrics.pdf",
                "metrics.pdf",
                "application/pdf",
                new ByteArrayInputStream(pdfBytes),
                Map.of()
        )));

        assertTrue(parsed.structureAware());
        assertNotNull(parsed.metadata().get("parseConfidence"));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("pageNumber")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_row".equals(block.blockType())));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.content().contains("Bob")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("evidenceAssetHint")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_summary".equals(block.blockType())));
        assertTrue(parsed.blocks().stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .anyMatch(block -> block.metadata().containsKey("tableGrid")));
        assertTrue(parsed.metadata().containsKey("pageWidths"));
        assertTrue(parsed.metadata().containsKey("pageHeights"));
    }

    private static byte[] buildTablePdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(72, 720);
                stream.showText("Name    Age");
                stream.endText();
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(72, 700);
                stream.showText("Alice   30");
                stream.endText();
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(72, 680);
                stream.showText("Bob     25");
                stream.endText();
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
