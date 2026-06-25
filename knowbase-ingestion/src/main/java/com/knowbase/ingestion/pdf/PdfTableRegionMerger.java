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
                combined.addAll(next.rows());
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
        return Math.abs(first.minX() - last.minX()) < 24f;
    }
}
