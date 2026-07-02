package com.knowbase.ingestion.pdf;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PdfTableCellBboxAssignerTest {

    @Test
    void assignsCellAndRegionBboxes() {
        List<PdfTableRowInput> rows = List.of(
                new PdfTableRowInput(1, 0, 0, 2, "Name    Age", 72, 700, 200, 12),
                new PdfTableRowInput(1, 1, 0, 2, "Alice   30", 72, 680, 200, 12)
        );
        List<PdfTableColumnDetector.ColumnBoundary> boundaries = PdfTableColumnDetector.detectFromBlocks(rows);
        List<Double> cellBbox = PdfTableCellBboxAssigner.cellBbox(rows.get(1), 0, 2, boundaries);
        assertEquals(4, cellBbox.size());
        List<Double> regionBbox = PdfTableCellBboxAssigner.tableRegionBbox(rows);
        assertEquals(4, regionBbox.size());

        List<StructuralBlock> blocks = PdfTableCellExtractor.toStructuralBlocks(rows, 1, 0);
        assertFalse(blocks.isEmpty());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cells = (List<Map<String, Object>>) blocks.getFirst().metadata().get("cellCoordinates");
        assertNotNull(cells);
        assertNotNull(cells.getFirst().get("bbox"));
        assertNotNull(blocks.getFirst().metadata().get("tableRegionBbox"));
        assertEquals(List.of(1, 1), blocks.getFirst().metadata().get("rowRange"));
    }
}
