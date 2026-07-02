package com.knowbase.ingestion.table;

import org.apache.poi.ss.util.CellReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains 1–N header rows and produces per-column header paths for tabular serialization.
 */
public final class MultiLevelHeaderStack {

    private final List<String[]> headerRows = new ArrayList<>();

    public void pushHeaderRow(List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        headerRows.add(values.toArray(String[]::new));
    }

    public void reset() {
        headerRows.clear();
    }

    public int headerRowCount() {
        return headerRows.size();
    }

    public String[] activeFlatHeaders(int columnCount) {
        if (headerRows.isEmpty()) {
            List<String> keys = columnKeys(columnCount);
            return keys.toArray(String[]::new);
        }
        String[] flat = new String[columnCount];
        for (String[] row : headerRows) {
            for (int index = 0; index < columnCount; index++) {
                String value = index < row.length ? row[index] : "";
                if (value != null && !value.isBlank()) {
                    flat[index] = value.trim();
                }
            }
        }
        for (int index = 0; index < columnCount; index++) {
            if (flat[index] == null || flat[index].isBlank()) {
                flat[index] = CellReference.convertNumToColString(index);
            }
        }
        return flat;
    }

    public List<String> headerPathForColumn(int columnIndex, int columnCount) {
        List<String> path = new ArrayList<>();
        String carry = "";
        for (String[] row : headerRows) {
            String value = columnIndex < row.length && row[columnIndex] != null ? row[columnIndex].trim() : "";
            if (!value.isBlank()) {
                carry = value;
            }
            if (!carry.isBlank()) {
                if (path.isEmpty() || !path.getLast().equals(carry)) {
                    path.add(carry);
                }
            }
        }
        if (path.isEmpty()) {
            path.add(CellReference.convertNumToColString(columnIndex));
        }
        return List.copyOf(path);
    }

    public List<String> columnKeys(int columnCount) {
        List<String> keys = new ArrayList<>(columnCount);
        for (int index = 0; index < columnCount; index++) {
            keys.add(CellReference.convertNumToColString(index));
        }
        return keys;
    }
}
