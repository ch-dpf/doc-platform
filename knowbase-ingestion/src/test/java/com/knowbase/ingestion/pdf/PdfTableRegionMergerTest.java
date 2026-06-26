package com.knowbase.ingestion.pdf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTableRegionMergerTest {

    @Test
    void mergesContinuationAcrossPages() {
        List<PdfTableRowInput> pageOne = List.of(row(1, "Name\tAge", 72f));
        List<PdfTableRowInput> pageTwo = List.of(row(2, "Bob\t30", 72f));
        List<PdfTableRegionMerger.PdfTableRegionSlice> merged = PdfTableRegionMerger.mergeAcrossPages(List.of(
                new PdfTableRegionMerger.PdfTableRegionSlice(0, pageOne),
                new PdfTableRegionMerger.PdfTableRegionSlice(1, pageTwo)
        ));
        assertEquals(1, merged.size());
        assertEquals(2, merged.getFirst().rows().size());
        assertEquals(0, merged.getFirst().tableRegionId());
    }

    @Test
    void keepsDistinctRegionsWhenColumnsDiffer() {
        List<PdfTableRowInput> first = List.of(row(1, "A\tB", 72f));
        List<PdfTableRowInput> second = List.of(row(2, "OnlyOneColumn", 72f));
        assertFalse(PdfTableRegionMerger.isContinuation(first, second));
    }

    @Test
    void mergesWhenNextPageRepeatsHeader() {
        List<PdfTableRowInput> pageOne = List.of(
                row(1, "Name\tAge", 72f, List.of(72f, 150f, 220f)),
                row(1, "Ann\t20", 72f, List.of(72f, 150f, 220f))
        );
        List<PdfTableRowInput> pageTwo = List.of(
                row(2, "Name\tAge", 72f, List.of(72f, 150f, 220f)),
                row(2, "Bob\t30", 72f, List.of(72f, 150f, 220f))
        );
        assertTrue(PdfTableRegionMerger.isContinuation(pageOne, pageTwo));
        List<PdfTableRegionMerger.PdfTableRegionSlice> merged = PdfTableRegionMerger.mergeAcrossPages(List.of(
                new PdfTableRegionMerger.PdfTableRegionSlice(0, pageOne),
                new PdfTableRegionMerger.PdfTableRegionSlice(0, pageTwo)
        ));
        assertEquals(1, merged.size());
        assertEquals(3, merged.getFirst().rows().size());
    }

    private static PdfTableRowInput row(int pageNumber, String content, float minX) {
        return new PdfTableRowInput(pageNumber, 0, 0, 1, content, minX, 700f, 200f, 12f);
    }

    private static PdfTableRowInput row(int pageNumber, String content, float minX, List<Float> boundaries) {
        return new PdfTableRowInput(pageNumber, 0, 0, 2, content, minX, 700f, 200f, 12f, boundaries);
    }
}
