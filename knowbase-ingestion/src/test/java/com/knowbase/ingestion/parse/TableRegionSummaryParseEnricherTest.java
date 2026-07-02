package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableRegionSummaryParseEnricherTest {

    @Test
    void injectsSummaryBeforeFirstRowOfEachRegion() {
        List<StructuralBlock> blocks = List.of(
                dataRow(1, "HEADER", "Name | Age"),
                dataRow(1, "DATA", "Alice | 30"),
                dataRow(2, "DATA", "Q1 | 100")
        );
        List<StructuralBlock> enriched = TableRegionSummaryParseEnricher.enrich(blocks);
        assertEquals(5, enriched.size());
        assertEquals("table_summary", enriched.get(0).blockType());
        assertTrue(enriched.get(0).content().contains("pdf-table-1"));
        assertEquals("table_row", enriched.get(1).blockType());
        assertEquals("table_summary", enriched.get(3).blockType());
        assertTrue(enriched.get(3).content().contains("pdf-table-2"));
    }

    private static StructuralBlock dataRow(int regionId, String rowRole, String content) {
        return new StructuralBlock(
                "table_row",
                0,
                content,
                regionId,
                Map.of(
                        "tableRegionId", regionId,
                        "tableRegionLabel", "pdf-table-" + regionId,
                        "rowRole", rowRole
                )
        );
    }
}
