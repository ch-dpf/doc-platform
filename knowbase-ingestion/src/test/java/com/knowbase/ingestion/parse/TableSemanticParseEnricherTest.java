package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableSemanticParseEnricherTest {

    @Test
    void stampsMergedCellSummaryMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("cellCoordinates", List.of(
                Map.of("columnIndex", 0, "columnSpan", 2, "merged", true, "value", "Total"),
                Map.of("columnIndex", 1, "columnSpan", 0, "merged", true, "mergeContinuation", true, "value", "")
        ));
        StructuralBlock block = new StructuralBlock("table_row", 0, "Total | 100", 0, metadata);
        List<StructuralBlock> enriched = TableSemanticParseEnricher.enrich(List.of(block));
        assertTrue(Boolean.TRUE.equals(enriched.getFirst().metadata().get("hasMergedCells")));
        assertEquals(2, enriched.getFirst().metadata().get("maxColumnSpan"));
    }
}
