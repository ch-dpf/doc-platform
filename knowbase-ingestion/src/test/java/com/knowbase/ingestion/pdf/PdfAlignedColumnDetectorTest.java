package com.knowbase.ingestion.pdf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PdfAlignedColumnDetectorTest {

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
