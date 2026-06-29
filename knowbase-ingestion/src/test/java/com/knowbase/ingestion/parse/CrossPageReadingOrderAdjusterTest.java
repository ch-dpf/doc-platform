package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossPageReadingOrderAdjusterTest {

    @Test
    void keepsMultiPageTableRowsContiguous() {
        List<StructuralBlock> blocks = List.of(
                paragraph("Intro", 0, 1),
                tableRow("row-a", 1, 1, 0),
                footerBlock("Footer page 1", 2, 1),
                tableRow("row-b", 3, 2, 1),
                paragraph("After table", 4, 1)
        );
        List<StructuralBlock> adjusted = CrossPageReadingOrderAdjuster.adjust(blocks);
        int rowA = indexOf(adjusted, "row-a");
        int rowB = indexOf(adjusted, "row-b");
        int footer = indexOf(adjusted, "Footer page 1");
        int after = indexOf(adjusted, "After table");
        assertTrue(rowB > rowA);
        assertTrue(footer > rowB);
        assertEquals(footer + 1, after);
    }

    private static int indexOf(List<StructuralBlock> blocks, String content) {
        for (int index = 0; index < blocks.size(); index++) {
            if (content.equals(blocks.get(index).content())) {
                return index;
            }
        }
        return -1;
    }

    private static StructuralBlock paragraph(String content, int order, int page) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("readingOrder", order);
        metadata.put("pageNumber", page);
        metadata.put("columnIndex", 0);
        metadata.put("columnCount", 1);
        return new StructuralBlock("paragraph", 0, content, order, Map.copyOf(metadata));
    }

    private static StructuralBlock footerBlock(String content, int order, int page) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("readingOrder", order);
        metadata.put("pageNumber", page);
        metadata.put("layoutRole", "footer");
        metadata.put("columnIndex", 0);
        metadata.put("columnCount", 1);
        return new StructuralBlock("paragraph", 0, content, order, Map.copyOf(metadata));
    }

    private static StructuralBlock tableRow(String content, int order, int page, int rowIndex) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("readingOrder", order);
        metadata.put("pageNumber", page);
        metadata.put("tableRegionId", 7);
        metadata.put("tableRegionRowIndex", rowIndex);
        metadata.put("columnIndex", 0);
        metadata.put("columnCount", 1);
        return new StructuralBlock("table_row", 0, content, order, Map.copyOf(metadata));
    }
}
