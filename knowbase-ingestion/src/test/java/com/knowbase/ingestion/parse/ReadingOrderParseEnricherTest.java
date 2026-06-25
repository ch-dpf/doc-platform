package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReadingOrderParseEnricherTest {

    @Test
    void assignsSequentialReadingOrder() {
        List<StructuralBlock> blocks = List.of(
                new StructuralBlock("paragraph", 0, "a", 0, Map.of()),
                new StructuralBlock("paragraph", 0, "b", 1, Map.of())
        );

        List<StructuralBlock> enriched = ReadingOrderParseEnricher.enrich(blocks);

        assertEquals(0, ((Number) enriched.get(0).metadata().get("readingOrder")).intValue());
        assertEquals(1, ((Number) enriched.get(1).metadata().get("readingOrder")).intValue());
    }
}
