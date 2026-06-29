package com.knowbase.ingestion.pdf;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a PDF table run into nested or side-by-side sub-regions (column-count / indent shifts).
 */
public final class PdfNestedTableSegmenter {

    private static final float NESTED_INDENT_THRESHOLD = 36f;
    private static final int COLUMN_SHIFT_THRESHOLD = 2;

    private PdfNestedTableSegmenter() {
    }

    public record TableSegment(List<PdfTableRowInput> rows, int nestedDepth, boolean nested, int segmentIndex) {
        public TableSegment(List<PdfTableRowInput> rows, int nestedDepth, boolean nested) {
            this(rows, nestedDepth, nested, 0);
        }
    }

    public static List<TableSegment> segment(List<PdfTableRowInput> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        if (rows.size() == 1) {
            return List.of(new TableSegment(List.copyOf(rows), 0, false, 0));
        }
        List<TableSegment> segments = new ArrayList<>();
        List<PdfTableRowInput> current = new ArrayList<>();
        int baseColumns = columnCount(rows.getFirst());
        float baseMinX = rows.getFirst().minX();
        int nestedDepth = 0;
        int segmentIndex = 0;

        for (PdfTableRowInput row : rows) {
            if (!current.isEmpty() && shouldSplit(current, row, baseColumns, baseMinX)) {
                segments.add(new TableSegment(List.copyOf(current), nestedDepth, nestedDepth > 0, segmentIndex++));
                current = new ArrayList<>();
                if (isNestedRelativeTo(row, baseMinX)) {
                    nestedDepth = Math.max(1, nestedDepth + 1);
                } else if (columnCount(row) != baseColumns) {
                    nestedDepth = 0;
                }
                baseColumns = columnCount(row);
                baseMinX = row.minX();
            }
            current.add(row);
        }
        if (!current.isEmpty()) {
            segments.add(new TableSegment(List.copyOf(current), nestedDepth, nestedDepth > 0, segmentIndex));
        }
        return List.copyOf(segments);
    }

    private static boolean shouldSplit(
            List<PdfTableRowInput> current,
            PdfTableRowInput row,
            int baseColumns,
            float baseMinX
    ) {
        if (isBlankRow(row)) {
            return true;
        }
        int rowColumns = columnCount(row);
        if (baseColumns >= 2 && rowColumns >= 2 && Math.abs(rowColumns - baseColumns) >= COLUMN_SHIFT_THRESHOLD) {
            return true;
        }
        if (isNestedRelativeTo(row, baseMinX) && rowColumns >= 2) {
            return true;
        }
        List<PdfTableColumnDetector.ColumnBoundary> currentBounds = PdfTableColumnDetector.detectFromBlocks(current);
        List<PdfTableColumnDetector.ColumnBoundary> rowBounds =
                PdfTableColumnDetector.detectFromBlocks(List.of(row));
        if (!currentBounds.isEmpty()
                && !rowBounds.isEmpty()
                && currentBounds.size() != rowBounds.size()
                && rowColumns >= 2) {
            return true;
        }
        return false;
    }

    private static boolean isNestedRelativeTo(PdfTableRowInput row, float baseMinX) {
        return row.minX() - baseMinX >= NESTED_INDENT_THRESHOLD;
    }

    private static boolean isBlankRow(PdfTableRowInput row) {
        return row.content() == null || row.content().isBlank();
    }

    private static int columnCount(PdfTableRowInput row) {
        return Math.max(1, PdfTableColumnDetector.splitCells(row.content()).size());
    }
}
