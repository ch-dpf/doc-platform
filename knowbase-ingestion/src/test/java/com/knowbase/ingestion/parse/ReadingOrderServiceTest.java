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
        assertEquals("heuristic-bbox", ordered.get(0).metadata().get("readingOrderSource"));
    }

    @Test
    void ordersMultiColumnBeforeNextColumn() {
        List<StructuralBlock> blocks = List.of(
                block("R1", 1, 1, List.of(320d, 700d, 100d, 20d), 0),
                block("L1", 1, 0, List.of(72d, 680d, 100d, 20d), 1),
                block("L2", 1, 0, List.of(72d, 700d, 100d, 20d), 2)
        );
        List<StructuralBlock> ordered = ReadingOrderService.apply(blocks, Map.of("readingOrderProvider", "heuristic"));
        assertEquals("L1", ordered.get(0).content());
        assertEquals("L2", ordered.get(1).content());
        assertEquals("R1", ordered.get(2).content());
    }

    private static StructuralBlock block(String text, int page, int columnIndex, List<Double> bbox, int ordinal) {
        return new StructuralBlock(
                "paragraph",
                0,
                text,
                ordinal,
                Map.of(
                        "pageNumber", page,
                        "columnIndex", columnIndex,
                        "columnCount", 2,
                        "multiColumn", true,
                        "bbox", bbox
                )
        );
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
