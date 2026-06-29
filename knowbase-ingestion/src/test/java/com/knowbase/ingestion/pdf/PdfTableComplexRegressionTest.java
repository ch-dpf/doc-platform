package com.knowbase.ingestion.pdf;

import com.knowbase.ingestion.DocumentSource;
import com.knowbase.ingestion.LayoutPdfTextExtractor;
import com.knowbase.ingestion.PdfLayoutParser;
import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PL-01 regression: ruled columns, nested sub-tables, cross-page continuation, cell bbox metadata.
 */
class PdfTableComplexRegressionTest {

    @Test
    void ruledTableRowsExposeRuledColumnCellBboxes() throws Exception {
        List<PdfTableRowInput> rows = List.of(
                row(1, "Name\tAge", 72f, List.of(72f, 150f, 220f)),
                row(1, "Alice\t30", 72f, List.of(72f, 148f, 218f)),
                row(1, "Bob\t25", 72f, List.of(72f, 152f, 222f))
        );
        List<StructuralBlock> blocks = PdfTableCellExtractor.toStructuralBlocks(rows, 7, 0, "ruled-column");
        assertEquals(2, blocks.size());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cells = (List<Map<String, Object>>) blocks.getFirst().metadata().get("cellCoordinates");
        assertNotNull(cells);
        assertEquals("pdf-ruled-column", cells.getFirst().get("bboxSource"));
        assertNotNull(cells.getFirst().get("bbox"));
        assertEquals("ruled-column", blocks.getFirst().metadata().get("tableDetection"));
    }

    @Test
    void nestedTableSegmentsReceiveDistinctRegionIdsAndParentLink() {
        List<PdfTableRowInput> rows = List.of(
                row(1, "A    B    C    D", 72),
                row(1, "1    2    3    4", 72),
                row(1, "X  Y", 120)
        );
        List<PdfNestedTableSegmenter.TableSegment> segments = PdfNestedTableSegmenter.segment(rows);
        assertEquals(2, segments.size());
        assertEquals(0, segments.getFirst().segmentIndex());
        assertTrue(segments.get(1).nested());
        assertTrue(segments.get(1).nestedDepth() >= 1);
    }

    @Test
    void continuationRowsInheritRuledBoundariesFromPreviousPage() {
        List<PdfTableRowInput> pageOne = List.of(
                row(1, "Name\tAge", 72f, List.of(72f, 150f, 220f)),
                row(1, "Ann\t20", 72f, List.of(72f, 150f, 220f))
        );
        List<PdfTableRowInput> pageTwo = List.of(row(2, "Bob\t30", 72f, List.of()));
        List<PdfTableRowInput> normalized = PdfTableRegionMerger.normalizeContinuationRows(pageOne, pageTwo);
        assertEquals(1, normalized.size());
        assertEquals(3, normalized.getFirst().cellBoundaryX().size());
    }

    @Test
    void layoutExtractorMergesCrossPageTableWithContinuationMetadata() throws Exception {
        List<StructuralBlock> blocks = LayoutPdfTextExtractor.extract(buildCrossPageTablePdf());
        List<StructuralBlock> tableRows = blocks.stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .toList();
        Set<Object> regionIds = tableRows.stream()
                .map(block -> block.metadata().get("tableRegionId"))
                .collect(Collectors.toSet());
        assertEquals(1, regionIds.size());
        assertTrue(tableRows.stream().anyMatch(block -> Boolean.TRUE.equals(block.metadata().get("tableContinuation"))));
        assertTrue(tableRows.stream().anyMatch(block -> block.metadata().containsKey("continuationOf")));
        assertTrue(tableRows.stream().anyMatch(block -> block.content().contains("Bob")));
    }

    @Test
    void enricherChainBuildsTableGridForRuledPdfTable() throws Exception {
        byte[] pdfBytes = buildRuledTablePdf();
        var parsed = ParsedDocumentParseEnricher.enrich(new PdfLayoutParser().parse(new DocumentSource(
                "memory://ruled-table.pdf",
                "ruled-table.pdf",
                "application/pdf",
                new java.io.ByteArrayInputStream(pdfBytes),
                Map.of()
        )));
        assertTrue(parsed.blocks().stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .anyMatch(block -> block.metadata().containsKey("tableGrid")));
        assertTrue(parsed.blocks().stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .anyMatch(block -> block.metadata().containsKey("cellCoordinates")));
    }

    private static byte[] buildRuledTablePdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeTabbedLine(stream, 72, 720, "Name", "Score");
                writeTabbedLine(stream, 72, 700, "Alice", "95");
                writeTabbedLine(stream, 72, 680, "Bob", "88");
            }
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static byte[] buildCrossPageTablePdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.addPage(new PDPage());
            try (PDPageContentStream stream = new PDPageContentStream(document, document.getPage(0))) {
                writeTabbedLine(stream, 72, 720, "Name", "Age");
            }
            try (PDPageContentStream stream = new PDPageContentStream(document, document.getPage(1))) {
                writeTabbedLine(stream, 72, 720, "Bob", "30");
            }
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static void writeTabbedLine(PDPageContentStream stream, float x, float y, String left, String right)
            throws Exception {
        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA, 12);
        stream.newLineAtOffset(x, y);
        stream.showText(padColumn(left, 12) + padColumn(right, 12).trim());
        stream.endText();
    }

    private static String padColumn(String value, int width) {
        if (value.length() >= width) {
            return value + "  ";
        }
        return value + " ".repeat(width - value.length());
    }

    private static PdfTableRowInput row(int page, String content, float minX) {
        return new PdfTableRowInput(page, 0, 0, 1, content, minX, 700f, 400f, 12f);
    }

    private static PdfTableRowInput row(int page, String content, float minX, List<Float> boundaries) {
        return new PdfTableRowInput(page, 0, 0, 2, content, minX, 700f, 200f, 12f, boundaries);
    }
}
