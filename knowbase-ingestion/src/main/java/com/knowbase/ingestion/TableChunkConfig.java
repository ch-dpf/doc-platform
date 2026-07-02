package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;

import java.util.Locale;
import java.util.Map;

/**
 * Resolved table chunking settings from L1 {@link LibraryProfile} + L2 {@link DocumentProfile}
 * and per-request overrides (same merge rules as {@link SegmentationConfigResolver}).
 */
public record TableChunkConfig(
        String chunkingStrategy,
        String chunkEngine,
        int chunkMaxTokens,
        int tableRowGroupMaxRows,
        int tableIndexMinFields,
        boolean prependSheetContext,
        RowGroupingMode rowGroupingMode
) {

    public enum RowGroupingMode {
        /** One parsed table row per chunk ({@code table_row}). */
        ONE_ROW,
        /** Merge consecutive rows until token budget ({@code table_row_token_window}, WeKnora-like). */
        TOKEN_WINDOW
    }

    public static TableChunkConfig resolve(
            LibraryProfile libraryProfile,
            DocumentProfile documentProfile,
            Map<String, Object> requestOptions
    ) {
        Map<String, Object> merged = SegmentationConfigResolver.mergeOptions(documentProfile, requestOptions);
        SegmentationConfig segmentation = libraryProfile == null
                ? SegmentationConfig.defaults(
                readInt(merged, "chunkMaxTokens", 384),
                readInt(merged, "chunkOverlapTokens", 48),
                stringOption(merged, "chunkingStrategy", documentProfile == null ? null : documentProfile.chunkingStrategy())
        )
                : SegmentationConfigResolver.resolve(libraryProfile, documentProfile, requestOptions);
        String strategy = segmentation.chunkingStrategy();
        if (strategy == null || strategy.isBlank()) {
            strategy = "table_row_token_window";
        }
        RowGroupingMode groupingMode = strategy.toLowerCase(Locale.ROOT).contains("table_row_token")
                ? RowGroupingMode.TOKEN_WINDOW
                : RowGroupingMode.ONE_ROW;
        int configuredMaxRows = readInt(merged, "tableRowGroupMaxRows", 1);
        int maxGroupRows = groupingMode == RowGroupingMode.TOKEN_WINDOW
                ? Integer.MAX_VALUE
                : Math.max(1, configuredMaxRows);
        return new TableChunkConfig(
                strategy,
                stringOption(merged, "chunkEngine", "smart"),
                segmentation.chunkMaxTokens(),
                maxGroupRows,
                readInt(merged, "tableIndexMinFields", TableRowIndexability.DEFAULT_MIN_FIELDS),
                readBoolean(merged, "prependSheetContext", true),
                groupingMode
        );
    }

    public boolean usesTokenWindowGrouping() {
        return rowGroupingMode == RowGroupingMode.TOKEN_WINDOW;
    }

    private static int readInt(Map<String, Object> options, String key, int defaultValue) {
        Object value = options.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static boolean readBoolean(Map<String, Object> options, String key, boolean defaultValue) {
        Object value = options.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value != null) {
            return Boolean.parseBoolean(String.valueOf(value).trim());
        }
        return defaultValue;
    }

    private static String stringOption(Map<String, Object> options, String key, String defaultValue) {
        Object value = options.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }
}
