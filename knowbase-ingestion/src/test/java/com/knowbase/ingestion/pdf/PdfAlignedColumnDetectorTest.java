package com.knowbase.ingestion.pdf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfAlignedColumnDetectorTest {

    @Test
    void skipsRowsWhenBoundaryPointsAreShorterThanColumnCount() {
        List<PdfTableRowInput> rows = List.of(
                row("A\tB\tC\tD", 72f, List.of(72f, 140f, 200f)),
                row("1\t2\t3\t4", 72f, List.of(72f, 138f, 198f)),
                row("a\tb\tc\td", 72f, List.of(72f, 142f, 202f))
        );
        List<PdfTableColumnDetector.ColumnBoundary> boundaries = assertDoesNotThrow(
                () -> PdfAlignedColumnDetector.detectAlignedBoundaries(rows, 4));
        assertTrue(boundaries.isEmpty());
    }

    @Test
    void detectsRuledBoundariesFromRepeatedCellStarts() {
        List<PdfTableRowInput> rows = List.of(
                row("Name\tAge", 72f, List.of(72f, 150f, 220f)),
                row("Alice\t30", 72f, List.of(72f, 148f, 218f)),
                row("Bob\t25", 72f, List.of(72f, 152f, 222f))
        );
        List<PdfTableColumnDetector.ColumnBoundary> boundaries =
                PdfAlignedColumnDetector.detectRuledBoundaries(rows, 2);
        assertEquals(2, boundaries.size());
        assertTrue(boundaries.getFirst().minX() >= 71f);
    }

    @Test
    void detectsAlignedBoundariesFromCellStarts() {
        List<PdfTableRowInput> rows = List.of(
                row("Name\tAge", 72f, List.of(72f, 140f, 200f)),
                row("Alice\t30", 72f, List.of(72f, 138f, 198f)),
                row("Bob\t25", 72f, List.of(72f, 142f, 202f))
        );
        List<PdfTableColumnDetector.ColumnBoundary> boundaries =
                PdfAlignedColumnDetector.detectAlignedBoundaries(rows, 2);
        assertEquals(2, boundaries.size());
        assertFalse(PdfAlignedColumnDetector.boundariesMatch(boundaries, List.of()));
    }

    private static PdfTableRowInput row(String content, float minX, List<Float> cellBoundaryX) {
        return new PdfTableRowInput(1, 0, 0, 1, content, minX, 700f, 120f, 12f, cellBoundaryX);
    }
}
