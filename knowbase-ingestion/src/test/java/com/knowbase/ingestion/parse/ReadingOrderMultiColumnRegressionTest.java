package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.LayoutPdfTextExtractor;
import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.domain.status.ContentFamily;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PL-02 regression: multi-column reading order and heuristic fallback metadata.
 */
class ReadingOrderMultiColumnRegressionTest {

    @Test
    void heuristicOrdersMultiColumnBlocksColumnMajor() {
        List<StructuralBlock> blocks = List.of(
                block("Right-1", 1, 1, 680, 320),
                block("Left-2", 1, 0, 700, 72),
                block("Left-1", 1, 0, 680, 72),
                block("Right-2", 1, 1, 700, 320)
        );
        List<StructuralBlock> ordered = ReadingOrderService.apply(blocks, Map.of("readingOrderProvider", "heuristic"));
        assertEquals("Left-1", ordered.get(0).content());
        assertEquals("Left-2", ordered.get(1).content());
        assertEquals("Right-1", ordered.get(2).content());
        assertEquals("Right-2", ordered.get(3).content());
        assertTrue(ordered.stream().allMatch(b -> "heuristic-bbox".equals(b.metadata().get("readingOrderSource"))));
        assertReadingOrderMonotonic(ordered);
    }

    @Test
    void layoutPdfMultiColumnReadingOrderIsColumnMajorAfterEnricher() throws Exception {
        List<StructuralBlock> raw = LayoutPdfTextExtractor.extract(buildTwoColumnPdf());
        List<StructuralBlock> enriched = ParsedDocumentParseEnricher.enrich(new ParsedDocument(
                "memory://two-col.pdf",
                "two-col.pdf",
                "body",
                ContentFamily.RICH_TEXT,
                Map.of("readingOrderProvider", "heuristic"),
                raw
        )).blocks();

        List<StructuralBlock> left = enriched.stream()
                .filter(block -> Integer.valueOf(0).equals(block.metadata().get("columnIndex")))
                .toList();
        List<StructuralBlock> right = enriched.stream()
                .filter(block -> Integer.valueOf(1).equals(block.metadata().get("columnIndex")))
                .toList();
        assertTrue(left.size() >= 2);
        assertTrue(right.size() >= 2);
        int maxLeftOrder = left.stream()
                .mapToInt(block -> ((Number) block.metadata().get("readingOrder")).intValue())
                .max()
                .orElse(0);
        int minRightOrder = right.stream()
                .mapToInt(block -> ((Number) block.metadata().get("readingOrder")).intValue())
                .min()
                .orElse(Integer.MAX_VALUE);
        assertTrue(maxLeftOrder < minRightOrder, "left column should precede right column globally");
        assertReadingOrderMonotonic(enriched);
    }

    @Test
    void fallsBackToHeuristicWhenOllamaUnavailable() {
        List<StructuralBlock> blocks = List.of(
                block("B", 1, 0, 200, 72),
                block("A", 1, 0, 100, 72)
        );
        List<StructuralBlock> ordered = ReadingOrderService.apply(blocks, Map.of(
                "readingOrderProvider", "ollama",
                "readingOrderOllamaModel", "knowbase-reading-order",
                "readingOrderOllamaBaseUrl", "http://127.0.0.1:1"
        ));
        assertEquals("A", ordered.get(0).content());
        assertEquals("heuristic-bbox", ordered.get(0).metadata().get("readingOrderSource"));
    }

    private static void assertReadingOrderMonotonic(List<StructuralBlock> blocks) {
        for (int index = 1; index < blocks.size(); index++) {
            int previous = ((Number) blocks.get(index - 1).metadata().get("readingOrder")).intValue();
            int current = ((Number) blocks.get(index).metadata().get("readingOrder")).intValue();
            assertTrue(current > previous, "readingOrder must increase: " + previous + " -> " + current);
        }
    }

    private static StructuralBlock block(String content, int page, int columnIndex, double top, double left) {
        Map<String, Object> metadata = Map.of(
                "pageNumber", page,
                "columnIndex", columnIndex,
                "columnCount", 2,
                "multiColumn", true,
                "bbox", List.of(left, top, 200d, 20d)
        );
        return new StructuralBlock("paragraph", 0, content, 0, metadata);
    }

    private static byte[] buildTwoColumnPdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 720, "Left column line one");
                writeLine(stream, 72, 700, "Left column line two");
                writeLine(stream, 320, 680, "Right column line one");
                writeLine(stream, 320, 660, "Right column line two");
            }
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
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
