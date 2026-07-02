package com.knowbase.ingestion.adaptive;

import com.knowbase.ingestion.table.MultiLevelHeaderStack;
import org.apache.poi.ss.util.CellReference;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Phase 3: renders rows into retrieval-oriented text by {@link TableRowRole}.
 */
public final class AdaptiveTableTextSerializer {

    private AdaptiveTableTextSerializer() {
    }

    public static String serialize(
            TableRowRole role,
            String sheetLabel,
            List<String> values,
            String[] columnHeaders
    ) {
        return serialize(role, sheetLabel, values, columnHeaders, null, columnHeaders == null ? 0 : columnHeaders.length);
    }

    public static String serialize(
            TableRowRole role,
            String sheetLabel,
            List<String> values,
            String[] columnHeaders,
            MultiLevelHeaderStack headerStack,
            int columnCount
    ) {
        return switch (role) {
            case LAYOUT -> serializeLayout(sheetLabel, values);
            case SEPARATOR -> serializeSeparator(sheetLabel, values);
            case FORM_KV -> serializeFormKv(sheetLabel, values);
            case HEADER -> serializeHeader(sheetLabel, values);
            case DATA -> serializeData(sheetLabel, values, columnHeaders, headerStack, columnCount);
            case COORDINATE -> serializeCoordinate(values);
        };
    }

    public static boolean defaultIndexable(TableRowRole role) {
        return switch (role) {
            case LAYOUT, HEADER, SEPARATOR -> false;
            case FORM_KV, DATA, COORDINATE -> true;
        };
    }

    private static String serializeLayout(String sheetLabel, List<String> values) {
        String title = layoutTitle(values);
        return prefix(sheetLabel, layoutFieldName(values)) + title;
    }

    private static String layoutTitle(List<String> values) {
        String uniform = AdaptiveTableLayoutAnalyzer.uniformPopulatedValue(values);
        if (uniform != null) {
            return uniform;
        }
        return joinNonBlank(values, " ");
    }

    /** Wide uniform rows = section break; sparse rows = document/sheet title. */
    private static String layoutFieldName(List<String> values) {
        int populated = AdaptiveTableLayoutAnalyzer.countPopulatedValues(values);
        if (AdaptiveTableLayoutAnalyzer.uniformPopulatedValue(values) != null && populated >= 3) {
            return "章节";
        }
        return "标题";
    }

    private static String serializeSeparator(String sheetLabel, List<String> values) {
        String period = joinNonBlank(values, " ");
        return prefix(sheetLabel, "汇报周期") + period;
    }

    private static String serializeFormKv(String sheetLabel, List<String> values) {
        List<String> pairs = extractFormPairs(values);
        if (pairs.isEmpty()) {
            return serializeCoordinate(values);
        }
        return sheetContext(sheetLabel, "元数据") + String.join(" | ", pairs);
    }

    private static String serializeHeader(String sheetLabel, List<String> values) {
        List<String> headers = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                headers.add(value.trim());
            }
        }
        return sheetContext(sheetLabel, "表头") + String.join(" | ", headers);
    }

    private static String serializeData(
            String sheetLabel,
            List<String> values,
            String[] columnHeaders,
            MultiLevelHeaderStack headerStack,
            int columnCount
    ) {
        if (columnHeaders == null || columnHeaders.length == 0) {
            return serializeCoordinate(values);
        }
        int populated = AdaptiveTableLayoutAnalyzer.countPopulatedValues(values);
        if (AdaptiveTableLayoutAnalyzer.isWideUniformLayoutRow(
                values,
                List.of(),
                columnHeaders,
                populated,
                RowLayoutContext.of(columnHeaders.length, 0)
        )) {
            return serializeLayout(sheetLabel, values);
        }
        List<String> fields = new ArrayList<>();
        int limit = Math.max(values.size(), columnHeaders.length);
        for (int index = 0; index < limit; index++) {
            String headerLabel = headerLabel(index, columnHeaders, headerStack, columnCount);
            String value = index < values.size() ? values.get(index) : "";
            if (headerLabel.isBlank() || value == null || value.isBlank()) {
                continue;
            }
            if (headerLabel.equals(value.trim())) {
                continue;
            }
            fields.add(headerLabel + ": " + value.trim());
        }
        if (fields.isEmpty()) {
            return serializeCoordinate(values);
        }
        return sheetContext(sheetLabel, null) + String.join(" | ", fields);
    }

    private static String headerLabel(
            int columnIndex,
            String[] columnHeaders,
            MultiLevelHeaderStack headerStack,
            int columnCount
    ) {
        if (headerStack != null && headerStack.headerRowCount() > 0) {
            List<String> path = headerStack.headerPathForColumn(columnIndex, columnCount);
            return path.stream().filter(value -> value != null && !value.isBlank()).collect(Collectors.joining("/"));
        }
        return columnIndex < columnHeaders.length && columnHeaders[columnIndex] != null
                ? columnHeaders[columnIndex].trim()
                : "";
    }

    /**
     * Fallback when no stable header row exists. Skips consecutive duplicate values from merged cells.
     */
    public static String serializeCoordinate(List<String> values) {
        StringBuilder builder = new StringBuilder();
        String previousValue = null;
        for (int index = 0; index < values.size(); index++) {
            String value = values.get(index);
            if (value == null || value.isBlank()) {
                continue;
            }
            if (value.equals(previousValue)) {
                continue;
            }
            previousValue = value;
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(CellReference.convertNumToColString(index)).append(": ").append(value);
        }
        return builder.toString();
    }

    static List<String> extractFormPairs(List<String> values) {
        List<String> pairs = new ArrayList<>();
        int index = 0;
        while (index < values.size()) {
            while (index < values.size() && isBlank(values.get(index))) {
                index++;
            }
            if (index >= values.size()) {
                break;
            }
            String label = values.get(index);
            if (!AdaptiveTableLayoutAnalyzer.isLikelyLabel(label)) {
                index++;
                continue;
            }
            int valueIndex = findNextNonBlankIndex(values, index + 1, 4);
            if (valueIndex < 0) {
                index++;
                continue;
            }
            String value = values.get(valueIndex);
            if (!isBlank(value) && !AdaptiveTableLayoutAnalyzer.isLikelyLabel(value)) {
                pairs.add(label.trim() + ": " + value.trim());
                index = valueIndex + 1;
            } else {
                index++;
            }
        }
        return pairs;
    }

    private static String sheetContext(String sheetLabel, String section) {
        String label = sheetLabel == null || sheetLabel.isBlank() ? "Table" : sheetLabel.trim();
        if (section == null || section.isBlank()) {
            return "[Sheet: " + label + "] ";
        }
        return "[Sheet: " + label + " | " + section + "] ";
    }

    private static String prefix(String sheetLabel, String fieldName) {
        return sheetContext(sheetLabel, null) + fieldName + ": ";
    }

    private static String joinNonBlank(List<String> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(delimiter);
            }
            builder.append(value.trim());
        }
        return builder.toString();
    }

    private static int findNextNonBlankIndex(List<String> values, int start, int maxLookahead) {
        int limit = Math.min(values.size(), start + maxLookahead);
        for (int index = start; index < limit; index++) {
            if (!isBlank(values.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
