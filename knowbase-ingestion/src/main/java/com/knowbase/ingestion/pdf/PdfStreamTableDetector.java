package com.knowbase.ingestion.pdf;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects stream-style table rows from whitespace-delimited text.
 */
public final class PdfStreamTableDetector {

    private PdfStreamTableDetector() {
    }

    public static boolean isStreamTableRow(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        if (content.contains("\t")) {
            return true;
        }
        List<String> cells = PdfTableColumnDetector.splitCells(content);
        return cells.size() >= 2;
    }

    public static boolean isAlignedWith(List<String> previousCells, List<String> currentCells) {
        if (previousCells == null || currentCells == null) {
            return false;
        }
        if (previousCells.size() < 2 || currentCells.size() < 2) {
            return false;
        }
        return previousCells.size() == currentCells.size();
    }

    public static List<String> cells(String content) {
        return new ArrayList<>(PdfTableColumnDetector.splitCells(content));
    }
}
