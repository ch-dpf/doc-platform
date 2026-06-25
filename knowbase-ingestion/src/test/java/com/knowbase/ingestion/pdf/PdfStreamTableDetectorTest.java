package com.knowbase.ingestion.pdf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfStreamTableDetectorTest {

    @Test
    void detectsTabSeparatedRows() {
        assertTrue(PdfStreamTableDetector.isStreamTableRow("Name\tAge"));
    }

    @Test
    void detectsWhitespaceAlignedRows() {
        assertTrue(PdfStreamTableDetector.isStreamTableRow("Alice   30"));
    }

    @Test
    void rejectsSingleColumnText() {
        assertFalse(PdfStreamTableDetector.isStreamTableRow("Introduction"));
    }

    @Test
    void requiresMatchingColumnCountsForAlignment() {
        List<String> previous = PdfStreamTableDetector.cells("Name    Age");
        List<String> current = PdfStreamTableDetector.cells("Bob     25");
        assertTrue(PdfStreamTableDetector.isAlignedWith(previous, current));
        assertFalse(PdfStreamTableDetector.isAlignedWith(previous, PdfStreamTableDetector.cells("Only")));
    }
}
