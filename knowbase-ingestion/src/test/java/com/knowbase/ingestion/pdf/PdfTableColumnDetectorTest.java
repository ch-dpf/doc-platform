package com.knowbase.ingestion.pdf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTableColumnDetectorTest {

    @Test
    void splitsCellsFromPipeDelimitedContent() {
        List<String> cells = PdfTableColumnDetector.splitCells("Name | Age | City");
        assertEquals(3, cells.size());
        assertEquals("Name", cells.get(0));
    }

    @Test
    void estimatesColumnCountFromContent() {
        List<PdfTableRowInput> rows = List.of(
                new PdfTableRowInput(1, 0, 0, 3, "A | B | C", 10, 100, 200, 12),
                new PdfTableRowInput(1, 1, 0, 3, "1 | 2 | 3", 10, 80, 200, 12)
        );
        assertTrue(PdfTableColumnDetector.estimateColumnCount(rows) >= 3);
    }

    @Test
    void alignsCellsToColumnCount() {
        List<String> cells = PdfTableColumnDetector.splitAlignedCells("A  B  C  D extra", 3);
        assertEquals(3, cells.size());
        assertEquals("C D extra", cells.get(2));
    }
}
