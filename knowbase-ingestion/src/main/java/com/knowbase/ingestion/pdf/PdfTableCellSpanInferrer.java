package com.knowbase.ingestion.pdf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Infers horizontal merged cells (columnSpan) for PDF table rows split into columns.
 */
public final class PdfTableCellSpanInferrer {

    private PdfTableCellSpanInferrer() {
    }

    public static List<Map<String, Object>> infer(List<Map<String, Object>> cells) {
        if (cells == null || cells.size() <= 1) {
            return cells == null ? List.of() : cells;
        }
        List<Map<String, Object>> working = new ArrayList<>(cells.size());
        for (Map<String, Object> cell : cells) {
            working.add(new HashMap<>(cell));
        }
        applyHorizontalMergeByDuplicateValue(working);
        applyHorizontalMergeByEmptyContinuation(working);
        return List.copyOf(working);
    }

    private static void applyHorizontalMergeByDuplicateValue(List<Map<String, Object>> cells) {
        int index = 0;
        while (index < cells.size()) {
            String value = stringValue(cells.get(index).get("value"));
            if (value.isBlank()) {
                index++;
                continue;
            }
            int span = 1;
            while (index + span < cells.size()) {
                String next = stringValue(cells.get(index + span).get("value"));
                if (!value.equals(next)) {
                    break;
                }
                span++;
            }
            if (span > 1) {
                markSpan(cells, index, span);
                index += span;
            } else {
                index++;
            }
        }
    }

    private static void applyHorizontalMergeByEmptyContinuation(List<Map<String, Object>> cells) {
        for (int index = 0; index < cells.size(); index++) {
            Map<String, Object> cell = cells.get(index);
            if (!stringValue(cell.get("value")).isBlank()) {
                continue;
            }
            if (index == 0) {
                continue;
            }
            Map<String, Object> previous = cells.get(index - 1);
            if (Boolean.TRUE.equals(previous.get("merged")) || intValue(previous.get("columnSpan")) > 1) {
                cell.put("merged", true);
                cell.put("columnSpan", 0);
                cell.put("mergeContinuation", true);
            }
        }
    }

    private static void markSpan(List<Map<String, Object>> cells, int start, int span) {
        cells.get(start).put("columnSpan", span);
        cells.get(start).put("merged", true);
        for (int offset = 1; offset < span; offset++) {
            Map<String, Object> covered = cells.get(start + offset);
            covered.put("columnSpan", 0);
            covered.put("merged", true);
            covered.put("mergeContinuation", true);
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
