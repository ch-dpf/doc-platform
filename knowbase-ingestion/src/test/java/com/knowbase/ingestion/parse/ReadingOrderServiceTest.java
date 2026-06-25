package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadingOrderServiceTest {

    @Test
    void ordersBlocksByPageAndBboxTop() {
        List<StructuralBlock> blocks = List.of(
                block("second", 1, List.of(0d, 200d, 100d, 220d), 1),
                block("first", 1, List.of(0d, 10d, 100d, 30d), 0)
        );
        List<StructuralBlock> ordered = ReadingOrderService.apply(blocks, Map.of());
        assertEquals("first", ordered.get(0).content());
        assertEquals("second", ordered.get(1).content());
        assertTrue(ordered.stream().allMatch(block -> block.metadata().containsKey("readingOrder")));
    }

    private static StructuralBlock block(String text, int page, List<Double> bbox, int ordinal) {
        return new StructuralBlock(
                "paragraph",
                0,
                text,
                ordinal,
                Map.of("pageNumber", page, "bbox", bbox, "ocrApplied", true)
        );
    }
}
