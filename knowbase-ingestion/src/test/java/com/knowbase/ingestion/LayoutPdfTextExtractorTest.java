package com.knowbase.ingestion;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutPdfTextExtractorTest {

    @Test
    void extractsLayoutBlocksFromSimplePdf() throws Exception {
        byte[] pdfBytes;
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                stream.newLineAtOffset(72, 720);
                stream.showText("Chapter 1 Overview");
                stream.endText();

                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(72, 680);
                stream.showText("This is body text for layout parsing.");
                stream.endText();
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            pdfBytes = outputStream.toByteArray();
        }

        List<StructuralBlock> blocks = LayoutPdfTextExtractor.extract(pdfBytes);
        assertFalse(blocks.isEmpty());
        assertTrue(blocks.stream().anyMatch(block -> block.content().contains("Overview")));
        assertTrue(blocks.stream().anyMatch(block -> Boolean.TRUE.equals(block.metadata().get("layoutParsing"))));
    }
}
