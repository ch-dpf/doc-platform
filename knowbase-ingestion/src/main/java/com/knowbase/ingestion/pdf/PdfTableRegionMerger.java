package com.knowbase.ingestion.pdf;

import java.util.ArrayList;
import java.util.List;

public final class PdfTableRegionMerger {

    private PdfTableRegionMerger() {
    }

    public record PdfTableRegionSlice(int tableRegionId, List<PdfTableRowInput> rows) {
    }

    public static List<PdfTableRegionSlice> mergeAcrossPages(List<PdfTableRegionSlice> regions) {
        if (regions == null || regions.size() <= 1) {
            return regions == null ? List.of() : regions;
        }
        List<PdfTableRegionSlice> merged = new ArrayList<>();
        PdfTableRegionSlice current = regions.getFirst();
        for (int index = 1; index < regions.size(); index++) {
            PdfTableRegionSlice next = regions.get(index);
            if (isContinuation(current.rows(), next.rows())) {
                List<PdfTableRowInput> combined = new ArrayList<>(current.rows());
                combined.addAll(normalizeContinuationRows(
                        current.rows(),
                        skipRepeatedHeaderRows(current.rows(), next.rows())
                ));
                current = new PdfTableRegionSlice(current.tableRegionId(), combined);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    public static boolean isContinuation(List<PdfTableRowInput> previous, List<PdfTableRowInput> next) {
        if (previous == null || next == null || previous.isEmpty() || next.isEmpty()) {
            return false;
        }
        PdfTableRowInput last = previous.getLast();
        PdfTableRowInput first = next.getFirst();
        if (first.pageNumber() <= last.pageNumber()) {
            return false;
        }
        int prevColumns = PdfTableColumnDetector.estimateColumnCount(previous);
        int nextColumns = PdfTableColumnDetector.estimateColumnCount(next);
        if (prevColumns != nextColumns) {
            return false;
        }
        List<PdfTableColumnDetector.ColumnBoundary> previousBounds = PdfTableColumnDetector.detectFromBlocks(previous);
        List<PdfTableColumnDetector.ColumnBoundary> nextBounds = PdfTableColumnDetector.detectFromBlocks(next);
        if (!previousBounds.isEmpty() && !nextBounds.isEmpty()) {
            return PdfAlignedColumnDetector.boundariesMatch(previousBounds, nextBounds);
        }
        return Math.abs(first.minX() - last.minX()) < 24f;
    }

    public static List<PdfTableRowInput> skipRepeatedHeaderRows(
            List<PdfTableRowInput> previous,
            List<PdfTableRowInput> next
    ) {
        if (next.isEmpty() || previous.isEmpty()) {
            return next == null ? List.of() : List.copyOf(next);
        }
        if (looksLikeRepeatedHeader(previous.getFirst(), next.getFirst())) {
            return next.size() <= 1 ? List.of() : List.copyOf(next.subList(1, next.size()));
        }
        return List.copyOf(next);
    }

    /**
     * Copies ruled column boundaries from the previous page when continuation rows lack TextPosition anchors.
     */
    public static List<PdfTableRowInput> normalizeContinuationRows(
            List<PdfTableRowInput> previous,
            List<PdfTableRowInput> next
    ) {
        if (previous == null || next == null || previous.isEmpty() || next.isEmpty()) {
            return next == null ? List.of() : List.copyOf(next);
        }
        PdfTableRowInput template = boundaryTemplate(previous);
        if (template == null) {
            return List.copyOf(next);
        }
        List<Float> boundaries = template.cellBoundaryX();
        int columnCount = PdfTableColumnDetector.estimateColumnCount(previous);
        if (boundaries.size() < columnCount + 1) {
            return List.copyOf(next);
        }
        List<PdfTableRowInput> normalized = new ArrayList<>(next.size());
        for (PdfTableRowInput row : next) {
            if (row.cellBoundaryX().size() >= columnCount + 1) {
                normalized.add(row);
                continue;
            }
            normalized.add(new PdfTableRowInput(
                    row.pageNumber(),
                    row.readingOrder(),
                    row.columnIndex(),
                    Math.max(row.columnCount(), columnCount),
                    row.content(),
                    row.minX(),
                    row.y(),
                    row.width(),
                    row.height(),
                    boundaries
            ));
        }
        return List.copyOf(normalized);
    }

    private static PdfTableRowInput boundaryTemplate(List<PdfTableRowInput> rows) {
        for (PdfTableRowInput row : rows) {
            if (row.cellBoundaryX().size() >= 3) {
                return row;
            }
        }
        return rows.getFirst();
    }

    private static List<PdfTableRowInput> skipRepeatedHeader(
            List<PdfTableRowInput> previous,
            List<PdfTableRowInput> next
    ) {
        return skipRepeatedHeaderRows(previous, next);
    }

    private static boolean looksLikeRepeatedHeader(PdfTableRowInput previousFirst, PdfTableRowInput nextFirst) {
        List<String> previousCells = PdfTableColumnDetector.splitCells(previousFirst.content());
        List<String> nextCells = PdfTableColumnDetector.splitCells(nextFirst.content());
        if (previousCells.size() < 2 || previousCells.size() != nextCells.size()) {
            return false;
        }
        int matches = 0;
        for (int index = 0; index < previousCells.size(); index++) {
            if (previousCells.get(index).equalsIgnoreCase(nextCells.get(index))) {
                matches++;
            }
        }
        return matches >= Math.max(2, previousCells.size() / 2);
    }
}
