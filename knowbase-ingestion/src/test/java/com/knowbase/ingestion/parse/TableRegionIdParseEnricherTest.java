package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableRegionIdParseEnricherTest {

    @Test
    void assignsRegionToOrphanTableRows() {
        List<StructuralBlock> blocks = List.of(
                new StructuralBlock("table_row", 0, "A | B", 0, Map.of("layoutRole", "table")),
                new StructuralBlock("table_row", 0, "1 | 2", 1, Map.of("layoutRole", "table")),
                new StructuralBlock("paragraph", 0, "text", 2, Map.of())
        );

        List<StructuralBlock> enriched = TableRegionIdParseEnricher.enrich(blocks);

        assertEquals(0, enriched.get(0).metadata().get("tableRegionId"));
        assertEquals(0, enriched.get(1).metadata().get("tableRegionId"));
        assertEquals("HEADER", enriched.get(0).metadata().get("rowRole"));
        assertEquals("DATA", enriched.get(1).metadata().get("rowRole"));
        assertTrue(enriched.stream().anyMatch(block -> "table_summary".equals(block.blockType())) == false);
    }
}
