package com.knowbase.ingestion.pdf;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTableCellSpanInferrerTest {

    @Test
    void infersHorizontalMergeFromDuplicateValues() {
        List<Map<String, Object>> cells = List.of(
                cell(0, "Total"),
                cell(1, "Total"),
                cell(2, "100")
        );
        List<Map<String, Object>> inferred = PdfTableCellSpanInferrer.infer(cells);
        assertEquals(2, intValue(inferred.getFirst().get("columnSpan")));
        assertTrue(Boolean.TRUE.equals(inferred.get(1).get("mergeContinuation")));
    }

    private static Map<String, Object> cell(int columnIndex, String value) {
        Map<String, Object> cell = new HashMap<>();
        cell.put("columnIndex", columnIndex);
        cell.put("value", value);
        cell.put("columnSpan", 1);
        cell.put("merged", false);
        return cell;
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
