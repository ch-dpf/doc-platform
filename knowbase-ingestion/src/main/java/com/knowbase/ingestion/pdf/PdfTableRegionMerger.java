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
                combined.addAll(skipRepeatedHeader(current.rows(), next.rows()));
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

    private static List<PdfTableRowInput> skipRepeatedHeader(
            List<PdfTableRowInput> previous,
            List<PdfTableRowInput> next
    ) {
        if (next.isEmpty() || previous.isEmpty()) {
            return next == null ? List.of() : next;
        }
        if (looksLikeRepeatedHeader(previous.getFirst(), next.getFirst())) {
            return next.size() <= 1 ? List.of() : List.copyOf(next.subList(1, next.size()));
        }
        return List.copyOf(next);
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
