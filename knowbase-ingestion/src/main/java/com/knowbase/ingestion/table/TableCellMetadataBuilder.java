package com.knowbase.ingestion.table;

import org.apache.poi.ss.util.CellReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds per-cell metadata maps aligned across spreadsheet, PDF, and office table parsers.
 */
public final class TableCellMetadataBuilder {

    private TableCellMetadataBuilder() {
    }

    public static List<Map<String, Object>> build(
            int rowIndex,
            List<String> values,
            List<String> columnKeys,
            List<Map<String, Object>> mergedCells,
            Map<Integer, String> formulaCells,
            Map<Integer, String> computedValues
    ) {
        List<Map<String, Object>> cells = new ArrayList<>();
        int size = Math.max(columnKeys.size(), values.size());
        for (int columnIndex = 0; columnIndex < size; columnIndex++) {
            String columnKey = columnIndex < columnKeys.size() && columnKeys.get(columnIndex) != null
                    && !columnKeys.get(columnIndex).isBlank()
                    ? columnKeys.get(columnIndex)
                    : CellReference.convertNumToColString(columnIndex);
            String value = columnIndex < values.size() && values.get(columnIndex) != null
                    ? values.get(columnIndex)
                    : "";
            Map<String, Object> cell = new HashMap<>();
            cell.put("rowIndex", rowIndex);
            cell.put("columnIndex", columnIndex);
            cell.put("coordinate", "R" + (rowIndex + 1) + "C" + (columnIndex + 1));
            cell.put("columnKey", columnKey);
            cell.put("headerPath", List.of(columnKey));
            cell.put("cellRef", CellReference.convertNumToColString(columnIndex) + (rowIndex + 1));
            cell.put("value", value.trim());
            if (formulaCells != null && formulaCells.containsKey(columnIndex)) {
                cell.put("formula", formulaCells.get(columnIndex));
            }
            if (computedValues != null && computedValues.containsKey(columnIndex)) {
                cell.put("computedValue", computedValues.get(columnIndex));
            }
            Map<String, Object> merged = mergedMetadataForCell(mergedCells, rowIndex, columnIndex);
            if (!merged.isEmpty()) {
                cell.put("merged", true);
                cell.put("mergedRange", merged.get("range"));
                cell.put("rowSpan", merged.get("rowSpan"));
                cell.put("columnSpan", merged.get("columnSpan"));
            } else {
                cell.put("merged", false);
            }
            cells.add(Map.copyOf(cell));
        }
        return cells;
    }

    public static List<Map<String, Object>> buildWithHeaderPaths(
            int rowIndex,
            List<String> values,
            MultiLevelHeaderStack headerStack,
            int columnCount,
            List<Map<String, Object>> mergedCells,
            Map<Integer, String> formulaCells,
            Map<Integer, String> computedValues
    ) {
        List<Map<String, Object>> cells = new ArrayList<>();
        int size = Math.max(columnCount, values.size());
        for (int columnIndex = 0; columnIndex < size; columnIndex++) {
            List<String> headerPath = headerStack.headerPathForColumn(columnIndex, columnCount);
            String columnKey = headerPath.getLast();
            String value = columnIndex < values.size() && values.get(columnIndex) != null
                    ? values.get(columnIndex)
                    : "";
            Map<String, Object> cell = new HashMap<>();
            cell.put("rowIndex", rowIndex);
            cell.put("columnIndex", columnIndex);
            cell.put("coordinate", "R" + (rowIndex + 1) + "C" + (columnIndex + 1));
            cell.put("columnKey", columnKey);
            cell.put("headerPath", headerPath);
            cell.put("cellRef", CellReference.convertNumToColString(columnIndex) + (rowIndex + 1));
            cell.put("value", value.trim());
            if (formulaCells != null && formulaCells.containsKey(columnIndex)) {
                cell.put("formula", formulaCells.get(columnIndex));
            }
            if (computedValues != null && computedValues.containsKey(columnIndex)) {
                cell.put("computedValue", computedValues.get(columnIndex));
            }
            Map<String, Object> merged = mergedMetadataForCell(mergedCells, rowIndex, columnIndex);
            if (!merged.isEmpty()) {
                cell.put("merged", true);
                cell.put("mergedRange", merged.get("range"));
                cell.put("rowSpan", merged.get("rowSpan"));
                cell.put("columnSpan", merged.get("columnSpan"));
            } else {
                cell.put("merged", false);
            }
            cells.add(Map.copyOf(cell));
        }
        return cells;
    }

    private static Map<String, Object> mergedMetadataForCell(
            List<Map<String, Object>> mergedCells,
            int rowIndex,
            int columnIndex
    ) {
        if (mergedCells == null || mergedCells.isEmpty()) {
            return Map.of();
        }
        for (Map<String, Object> range : mergedCells) {
            int rowStart = intValue(range.get("rowStart"), -1);
            int rowEnd = intValue(range.get("rowEnd"), -1);
            int columnStart = intValue(range.get("columnStart"), -1);
            int columnEnd = intValue(range.get("columnEnd"), -1);
            if (rowStart <= rowIndex && rowIndex <= rowEnd && columnStart <= columnIndex && columnIndex <= columnEnd) {
                return range;
            }
        }
        return Map.of();
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
