package com.knowbase.ingestion.pdf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Derives stable column boundaries from vertically aligned cell starts across table rows.
 */
public final class PdfAlignedColumnDetector {

    private static final float ALIGN_TOLERANCE = 8f;

    private PdfAlignedColumnDetector() {
    }

    public static List<PdfTableColumnDetector.ColumnBoundary> detectAlignedBoundaries(
            List<PdfTableRowInput> rows,
            int columnCount
    ) {
        if (rows == null || rows.isEmpty() || columnCount <= 1) {
            return List.of();
        }
        List<List<Float>> startsByColumn = new ArrayList<>();
        List<List<Float>> endsByColumn = new ArrayList<>();
        for (int index = 0; index < columnCount; index++) {
            startsByColumn.add(new ArrayList<>());
            endsByColumn.add(new ArrayList<>());
        }
        int alignedRows = 0;
        for (PdfTableRowInput row : rows) {
            List<Float> boundaries = row.cellBoundaryX();
            if (boundaries.size() < 2) {
                continue;
            }
            List<String> cells = PdfTableColumnDetector.splitCells(row.content());
            boolean cellsMatch = cells.size() == columnCount;
            boolean boundariesMatch = boundaries.size() - 1 == columnCount;
            if (!cellsMatch && !boundariesMatch) {
                continue;
            }
            if (boundaries.size() < columnCount) {
                continue;
            }
            alignedRows++;
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                startsByColumn.get(columnIndex).add(boundaries.get(columnIndex));
                float end = columnIndex + 1 < boundaries.size()
                        ? boundaries.get(columnIndex + 1)
                        : row.minX() + row.width();
                endsByColumn.get(columnIndex).add(end);
            }
        }
        if (alignedRows < 2) {
            return List.of();
        }
        List<PdfTableColumnDetector.ColumnBoundary> boundaries = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            List<Float> starts = startsByColumn.get(columnIndex);
            List<Float> ends = endsByColumn.get(columnIndex);
            if (starts.isEmpty() || ends.isEmpty()) {
                return List.of();
            }
            float minX = median(starts);
            float maxX = median(ends);
            if (maxX - minX < 1f) {
                maxX = minX + 1f;
            }
            boundaries.add(new PdfTableColumnDetector.ColumnBoundary(columnIndex, minX, maxX));
        }
        return boundaries;
    }

    /**
     * Derives stable column boundaries from repeated per-row TextPosition cell starts (ruled tables).
     */
    public static List<PdfTableColumnDetector.ColumnBoundary> detectRuledBoundaries(
            List<PdfTableRowInput> rows,
            int columnCount
    ) {
        if (rows == null || rows.isEmpty() || columnCount <= 1) {
            return List.of();
        }
        List<List<Float>> boundarySets = new ArrayList<>();
        for (PdfTableRowInput row : rows) {
            List<Float> boundaries = row.cellBoundaryX();
            if (boundaries.size() >= columnCount + 1) {
                boundarySets.add(boundaries.subList(0, columnCount + 1));
            }
        }
        if (boundarySets.size() < 2) {
            return List.of();
        }
        List<PdfTableColumnDetector.ColumnBoundary> result = new ArrayList<>(columnCount);
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            List<Float> starts = new ArrayList<>(boundarySets.size());
            List<Float> ends = new ArrayList<>(boundarySets.size());
            for (List<Float> set : boundarySets) {
                starts.add(set.get(columnIndex));
                ends.add(set.get(columnIndex + 1));
            }
            float minX = median(starts);
            float maxX = median(ends);
            if (maxX <= minX) {
                maxX = minX + 1f;
            }
            result.add(new PdfTableColumnDetector.ColumnBoundary(columnIndex, minX, maxX));
        }
        return result;
    }

    public static boolean boundariesMatch(
            List<PdfTableColumnDetector.ColumnBoundary> left,
            List<PdfTableColumnDetector.ColumnBoundary> right
    ) {
        if (left == null || right == null || left.size() != right.size() || left.isEmpty()) {
            return false;
        }
        for (int index = 0; index < left.size(); index++) {
            PdfTableColumnDetector.ColumnBoundary a = left.get(index);
            PdfTableColumnDetector.ColumnBoundary b = right.get(index);
            if (Math.abs(a.minX() - b.minX()) > ALIGN_TOLERANCE
                    || Math.abs(a.maxX() - b.maxX()) > ALIGN_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    private static float median(List<Float> values) {
        List<Float> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return (sorted.get(middle - 1) + sorted.get(middle)) / 2f;
        }
        return sorted.get(middle);
    }
}
