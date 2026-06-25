package com.knowbase.ingestion;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(blocks.stream().allMatch(block -> block.metadata().containsKey("pageNumber")));
        assertTrue(blocks.stream().allMatch(block -> block.metadata().containsKey("bbox")));
        assertTrue(blocks.stream().allMatch(block -> block.metadata().containsKey("readingOrder")));
    }

    @Test
    void detectsMultiColumnLayoutHints() throws Exception {
        byte[] pdfBytes;
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 720, "Left column line one");
                writeLine(stream, 72, 700, "Left column line two");
                writeLine(stream, 320, 680, "Right column line one");
                writeLine(stream, 320, 660, "Right column line two");
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            pdfBytes = outputStream.toByteArray();
        }

        List<StructuralBlock> blocks = LayoutPdfTextExtractor.extract(pdfBytes);

        assertFalse(blocks.isEmpty());
        assertTrue(blocks.stream().anyMatch(block -> Boolean.TRUE.equals(block.metadata().get("multiColumn"))));
        assertEquals(2, blocks.stream()
                .map(block -> block.metadata().get("columnCount"))
                .filter(Integer.class::isInstance)
                .mapToInt(Integer.class::cast)
                .max()
                .orElse(0));
    }

    @Test
    void marksFooterBlocksAsNonIndexable() throws Exception {
        List<StructuralBlock> blocks = LayoutPdfTextExtractor.extract(buildTwoPagePdfWithFooter());
        assertTrue(blocks.stream().anyMatch(block -> "footer".equals(block.metadata().get("layoutRole"))));
        assertTrue(blocks.stream()
                .filter(block -> "footer".equals(block.metadata().get("layoutRole")))
                .allMatch(block -> Boolean.FALSE.equals(block.metadata().get("indexableHint"))));
    }

    @Test
    void mergesTableRegionsAcrossPageBreakWhenInterruptedByFooter() throws Exception {
        List<StructuralBlock> blocks = LayoutPdfTextExtractor.extract(buildCrossPageTablePdf());
        long regionIds = blocks.stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .map(block -> block.metadata().get("tableRegionId"))
                .distinct()
                .count();
        assertEquals(1, regionIds);
        assertTrue(blocks.stream().anyMatch(block -> block.content().contains("Bob")));
    }

    private static byte[] buildTwoPagePdfWithFooter() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 720, "Body paragraph for footer detection.");
                writeLine(stream, 72, 36, "Page 1");
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static byte[] buildCrossPageTablePdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.addPage(new PDPage());
            try (PDPageContentStream stream = new PDPageContentStream(document, document.getPage(0))) {
                writeLine(stream, 72, 720, "Name    Age");
                writeLine(stream, 300, 24, "1");
            }
            try (PDPageContentStream stream = new PDPageContentStream(document, document.getPage(1))) {
                writeLine(stream, 72, 720, "Bob    30");
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static void writeLine(PDPageContentStream stream, float x, float y, String text) throws Exception {
        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA, 12);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }
}
