package com.knowbase.ingestion;

/**
 * Heuristic indexability for flattened table rows (A: val,B: val,...).
 * Layout lines with few populated columns stay in the document but skip vector indexing.
 */
public final class TableRowIndexability {

    public static final int DEFAULT_MIN_FIELDS = 4;

    private TableRowIndexability() {
    }

    public static boolean isIndexable(String rowText, int minPopulatedFields, int sheetColumnCount) {
        int threshold = resolveThreshold(minPopulatedFields, sheetColumnCount);
        if (threshold <= 0) {
            return true;
        }
        return countPopulatedFields(rowText) >= threshold;
    }

    static int resolveThreshold(int minPopulatedFields, int sheetColumnCount) {
        int configured = minPopulatedFields <= 0 ? DEFAULT_MIN_FIELDS : minPopulatedFields;
        if (sheetColumnCount <= 4) {
            return Math.max(2, Math.min(3, configured));
        }
        if (sheetColumnCount <= 7) {
            return Math.min(4, configured);
        }
        return configured;
    }

    public static int countPopulatedFields(String rowText) {
        if (rowText == null || rowText.isBlank()) {
            return 0;
        }
        String normalized = StructuredTableSummaryBuilder.stripPrefixes(rowText.trim());
        if (normalized.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String segment : normalized.split("[|,]")) {
            String token = segment.trim();
            int separator = token.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String value = token.substring(separator + 1).trim();
            if (!value.isBlank()) {
                count++;
            }
        }
        return count;
    }
}
