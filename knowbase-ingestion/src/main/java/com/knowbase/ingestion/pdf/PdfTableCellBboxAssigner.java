package com.knowbase.ingestion.pdf;

import java.util.ArrayList;
import java.util.List;

/**
 * Assigns per-cell and table-region bounding boxes for PDF stream tables.
 */
public final class PdfTableCellBboxAssigner {

    private PdfTableCellBboxAssigner() {
    }

    public static List<Double> cellBbox(
            PdfTableRowInput row,
            int columnIndex,
            int columnCount,
            List<PdfTableColumnDetector.ColumnBoundary> boundaries
    ) {
        if (row == null || columnCount <= 0 || columnIndex < 0 || columnIndex >= columnCount) {
            return List.of();
        }
        float y = row.y();
        float height = Math.max(1f, row.height());
        if (boundaries != null
                && boundaries.size() == columnCount
                && columnIndex < boundaries.size()) {
            PdfTableColumnDetector.ColumnBoundary boundary = boundaries.get(columnIndex);
            float minX = boundary.minX();
            float width = Math.max(1f, boundary.maxX() - boundary.minX());
            return List.of(round(minX), round(y), round(width), round(height));
        }
        float segmentWidth = Math.max(1f, row.width() / columnCount);
        float minX = row.minX() + columnIndex * segmentWidth;
        return List.of(round(minX), round(y), round(segmentWidth), round(height));
    }

    public static List<Double> tableRegionBbox(List<PdfTableRowInput> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        for (PdfTableRowInput row : rows) {
            minX = Math.min(minX, row.minX());
            minY = Math.min(minY, row.y());
            maxX = Math.max(maxX, row.minX() + row.width());
            maxY = Math.max(maxY, row.y() + row.height());
        }
        return List.of(round(minX), round(minY), round(maxX - minX), round(maxY - minY));
    }

    public static List<Integer> rowRange(int rowIndex) {
        return List.of(rowIndex, rowIndex);
    }

    public static List<Integer> columnRange(int columnCount) {
        if (columnCount <= 0) {
            return List.of();
        }
        return List.of(0, columnCount - 1);
    }

    private static double round(float value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
