package com.knowbase.ingestion.pdf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfNestedTableSegmenterTest {

    @Test
    void splitsWhenColumnCountChanges() {
        List<PdfTableRowInput> rows = List.of(
                row(1, "A    B    C    D", 72),
                row(1, "1    2    3    4", 72),
                row(1, "X  Y", 120)
        );
        List<PdfNestedTableSegmenter.TableSegment> segments = PdfNestedTableSegmenter.segment(rows);
        assertEquals(2, segments.size());
        assertEquals(2, segments.getFirst().rows().size());
        assertTrue(segments.get(1).nested());
    }

    @Test
    void keepsSingleRegionWhenColumnsStable() {
        List<PdfTableRowInput> rows = List.of(
                row(1, "H1   H2   H3", 72),
                row(1, "v1   v2   v3", 72)
        );
        assertEquals(1, PdfNestedTableSegmenter.segment(rows).size());
    }

    private static PdfTableRowInput row(int page, String content, float minX) {
        return new PdfTableRowInput(page, 0, 0, 1, content, minX, 700f, 400f, 12f);
    }
}
