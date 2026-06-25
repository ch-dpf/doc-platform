package com.knowbase.ingestion.table;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableGridModelTest {

    @Test
    void buildsGridFromPdfTableRows() {
        List<StructuralBlock> rows = List.of(
                tableRow(0, "HEADER", List.of(
                        cell(0, 0, "Name", List.of("Name")),
                        cell(0, 1, "Age", List.of("Age"))
                )),
                tableRow(1, "DATA", List.of(
                        cell(1, 0, "Alice", List.of("Name")),
                        cell(1, 1, "30", List.of("Age"))
                ))
        );
        TableGridModel.Grid grid = TableGridModel.fromTableRows(7, rows);
        assertEquals(7, grid.tableRegionId());
        assertEquals(2, grid.rowCount());
        assertEquals(2, grid.columnCount());
        assertEquals("Alice", grid.rows().get(1).cells().get(0).value());
        Map<String, Object> metadata = TableGridModel.toMetadata(grid);
        assertTrue(metadata.containsKey("tableGrid"));
        assertEquals(2, metadata.get("tableGridRowCount"));
    }

    private static StructuralBlock tableRow(int rowIndex, String rowRole, List<Map<String, Object>> cells) {
        return new StructuralBlock(
                "table_row",
                0,
                "row",
                rowIndex,
                Map.of(
                        "tableRegionId", 7,
                        "tableRegionLabel", "pdf-table-7",
                        "tableRegionRowIndex", rowIndex,
                        "rowRole", rowRole,
                        "cellCoordinates", cells
                )
        );
    }

    private static Map<String, Object> cell(int rowIndex, int columnIndex, String value, List<String> headerPath) {
        return Map.of(
                "rowIndex", rowIndex,
                "columnIndex", columnIndex,
                "value", value,
                "headerPath", headerPath,
                "rowSpan", 1,
                "columnSpan", 1
        );
    }
}
