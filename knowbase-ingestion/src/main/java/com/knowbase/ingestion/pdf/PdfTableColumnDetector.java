package com.knowbase.ingestion.pdf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PdfTableColumnDetector {

    private static final float MIN_COLUMN_GAP = 12f;

    private PdfTableColumnDetector() {
    }

    public record ColumnBoundary(int index, float minX, float maxX) {
    }

    public static List<ColumnBoundary> detectFromBlocks(List<PdfTableRowInput> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        int columnCount = estimateColumnCount(rows);
        List<ColumnBoundary> aligned = PdfAlignedColumnDetector.detectAlignedBoundaries(rows, columnCount);
        if (!aligned.isEmpty()) {
            return aligned;
        }
        List<ColumnBoundary> ruled = PdfAlignedColumnDetector.detectRuledBoundaries(rows, columnCount);
        if (!ruled.isEmpty()) {
            return ruled;
        }
        return detectFromRowStarts(rows);
    }

    private static List<ColumnBoundary> detectFromRowStarts(List<PdfTableRowInput> rows) {
        List<Float> anchors = new ArrayList<>();
        for (PdfTableRowInput row : rows) {
            anchors.add(row.minX());
        }
        anchors.sort(Comparator.naturalOrder());
        List<ColumnBoundary> boundaries = new ArrayList<>();
        float clusterStart = anchors.getFirst();
        float clusterEnd = anchors.getFirst();
        int index = 0;
        for (int pointer = 1; pointer < anchors.size(); pointer++) {
            float value = anchors.get(pointer);
            if (value - clusterEnd > MIN_COLUMN_GAP) {
                boundaries.add(new ColumnBoundary(index++, clusterStart, clusterEnd));
                clusterStart = value;
            }
            clusterEnd = value;
        }
        boundaries.add(new ColumnBoundary(index, clusterStart, clusterEnd));
        return boundaries;
    }

    public static int estimateColumnCount(List<PdfTableRowInput> rows) {
        if (rows == null || rows.isEmpty()) {
            return 1;
        }
        int maxFromBoundaries = 1;
        int maxCells = 1;
        for (PdfTableRowInput row : rows) {
            maxCells = Math.max(maxCells, splitCells(row.content()).size());
            List<Float> boundaries = row.cellBoundaryX();
            if (boundaries.size() >= 2) {
                maxFromBoundaries = Math.max(maxFromBoundaries, boundaries.size() - 1);
            }
        }
        if (maxFromBoundaries >= 2) {
            return Math.max(maxCells, maxFromBoundaries);
        }
        List<ColumnBoundary> boundaries = detectFromRowStarts(rows);
        if (boundaries.isEmpty()) {
            return Math.max(1, maxCells);
        }
        return Math.max(maxCells, boundaries.size());
    }

    static List<String> splitCells(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        if (content.contains("|")) {
            return List.of(content.split("\\s\\|\\s"));
        }
        if (content.contains("\t")) {
            return List.of(content.split("\t"));
        }
        return List.of(content.trim().split("\\s{2,}"));
    }

    /**
     * Splits row text and pads or merges cells to match the detected column count.
     */
    public static List<String> splitAlignedCells(String content, int columnCount) {
        List<String> cells = new ArrayList<>(splitCells(content));
        if (columnCount <= 0) {
            return cells;
        }
        while (cells.size() < columnCount) {
            cells.add("");
        }
        if (cells.size() > columnCount) {
            String overflow = String.join(" ", cells.subList(columnCount - 1, cells.size()));
            cells = new ArrayList<>(cells.subList(0, columnCount - 1));
            cells.add(overflow);
        }
        return cells;
    }
}
