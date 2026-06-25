package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableGridParseEnricherTest {

    @Test
    void stampsTableGridOnFirstRowOnly() {
        List<StructuralBlock> blocks = List.of(
                new StructuralBlock("table_row", 0, "Name | Age", 0, Map.of(
                        "tableRegionId", 1,
                        "tableRegionLabel", "pdf-table-1",
                        "tableRegionRowIndex", 0,
                        "rowRole", "HEADER",
                        "cellCoordinates", List.of(
                                Map.of("rowIndex", 0, "columnIndex", 0, "value", "Name", "headerPath", List.of("Name")),
                                Map.of("rowIndex", 0, "columnIndex", 1, "value", "Age", "headerPath", List.of("Age"))
                        )
                )),
                new StructuralBlock("table_row", 0, "Alice | 30", 1, Map.of(
                        "tableRegionId", 1,
                        "tableRegionLabel", "pdf-table-1",
                        "tableRegionRowIndex", 1,
                        "rowRole", "DATA",
                        "cellCoordinates", List.of(
                                Map.of("rowIndex", 1, "columnIndex", 0, "value", "Alice", "headerPath", List.of("Name")),
                                Map.of("rowIndex", 1, "columnIndex", 1, "value", "30", "headerPath", List.of("Age"))
                        )
                ))
        );
        List<StructuralBlock> enriched = TableGridParseEnricher.enrich(blocks);
        assertTrue(enriched.getFirst().metadata().containsKey("tableGrid"));
        assertFalse(enriched.get(1).metadata().containsKey("tableGrid"));
    }
}
