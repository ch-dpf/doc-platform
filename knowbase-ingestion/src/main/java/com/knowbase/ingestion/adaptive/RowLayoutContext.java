package com.knowbase.ingestion.adaptive;

/**
 * Physical layout hints for one spreadsheet row (language-agnostic).
 */
public record RowLayoutContext(int sheetColumnCount, int horizontalMergeSpan) {

    public static RowLayoutContext none() {
        return new RowLayoutContext(0, 0);
    }

    public static RowLayoutContext of(int sheetColumnCount, int horizontalMergeSpan) {
        return new RowLayoutContext(Math.max(0, sheetColumnCount), Math.max(0, horizontalMergeSpan));
    }
}
