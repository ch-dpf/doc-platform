package com.knowbase.ingestion.adaptive;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Phase 2: assigns a {@link TableRowRole} to each populated row in a sheet.
 */
final class AdaptiveTableLayoutAnalyzer {

    private static final Pattern DATE_RANGE = Pattern.compile(
            "\\d{4}\\s*年?\\s*\\d{1,2}\\s*月?\\s*\\d{1,2}\\s*日?\\s*[-–—~至到]+",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MOSTLY_NUMERIC = Pattern.compile("^[\\d.,]+$");
    private static final Set<String> LABEL_HINTS = Set.of(
            "部门", "姓名", "名称", "更新日期", "汇报周期", "项目负责人", "负责人", "日期", "周期", "编制人"
    );
    private static final Set<String> HEADER_HINTS = Set.of(
            "序号", "项目", "内容", "情况", "时间", "责任", "要求", "名称", "region", "quarter", "name", "q1", "q2"
    );

    private AdaptiveTableLayoutAnalyzer() {
    }

    static TableRowRole detectRole(
            List<String> values,
            List<String> nextRowValues,
            String[] activeColumnHeaders,
            int populatedCount
    ) {
        return detectRole(values, nextRowValues, activeColumnHeaders, populatedCount, RowLayoutContext.none());
    }

    static TableRowRole detectRole(
            List<String> values,
            List<String> nextRowValues,
            String[] activeColumnHeaders,
            int populatedCount,
            RowLayoutContext layoutContext
    ) {
        if (populatedCount == 0) {
            return TableRowRole.COORDINATE;
        }
        if (isLayoutRow(values, populatedCount)) {
            return TableRowRole.LAYOUT;
        }
        if (isFormKvRow(values, populatedCount, nextRowValues)) {
            return TableRowRole.FORM_KV;
        }
        if (isSeparatorRow(values, populatedCount)) {
            return TableRowRole.SEPARATOR;
        }
        if (isWideUniformLayoutRow(values, nextRowValues, activeColumnHeaders, populatedCount, layoutContext)) {
            return TableRowRole.LAYOUT;
        }
        if (activeColumnHeaders != null && isDataRow(values, activeColumnHeaders, populatedCount, layoutContext)) {
            return TableRowRole.DATA;
        }
        if (isHeaderRow(values, nextRowValues, populatedCount)) {
            return TableRowRole.HEADER;
        }
        if (activeColumnHeaders != null) {
            return TableRowRole.DATA;
        }
        return TableRowRole.COORDINATE;
    }

    static String[] buildColumnHeaders(List<String> values) {
        String[] headers = new String[values.size()];
        for (int index = 0; index < values.size(); index++) {
            String value = values.get(index);
            headers[index] = value == null || value.isBlank() ? "" : value.trim();
        }
        return headers;
    }

    /**
     * Returns the shared non-blank cell text when at least two populated cells are identical
     * (typical of horizontally merged cells expanded across columns).
     */
    static String uniformPopulatedValue(List<String> values) {
        String uniform = null;
        int populated = 0;
        for (String value : values) {
            if (isBlank(value)) {
                continue;
            }
            String trimmed = value.trim();
            populated++;
            if (uniform == null) {
                uniform = trimmed;
            } else if (!uniform.equals(trimmed)) {
                return null;
            }
        }
        return populated >= 2 ? uniform : null;
    }

    static int countPopulatedValues(List<String> values) {
        return countPopulated(values);
    }

    /**
     * Language-agnostic: merged / wide rows where every populated cell repeats the same non-tabular text.
     */
    static boolean isWideUniformLayoutRow(
            List<String> values,
            List<String> nextRowValues,
            String[] activeColumnHeaders,
            int populatedCount,
            RowLayoutContext layoutContext
    ) {
        String uniform = uniformPopulatedValue(values);
        if (uniform == null || populatedCount < 2) {
            return false;
        }
        if (MOSTLY_NUMERIC.matcher(uniform).matches()) {
            return false;
        }
        RowLayoutContext context = layoutContext == null ? RowLayoutContext.none() : layoutContext;
        int mergeSpan = context.horizontalMergeSpan();
        int sheetColumns = context.sheetColumnCount();
        if (uniform.length() <= 3 && mergeSpan < 2) {
            return false;
        }
        if (isHeaderRow(values, nextRowValues, populatedCount)) {
            return false;
        }
        if (!isWideUniformSpread(populatedCount, sheetColumns, mergeSpan)) {
            return false;
        }
        if (nextRowValues != null && isHeaderRow(nextRowValues, List.of(), countPopulated(nextRowValues))) {
            return true;
        }
        if (activeColumnHeaders != null) {
            return true;
        }
        return mergeSpan >= 2 || populatedCount >= 3;
    }

    static boolean isLikelyLabel(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String trimmed = text.trim();
        if (trimmed.length() > 16) {
            return false;
        }
        if (MOSTLY_NUMERIC.matcher(trimmed).matches()) {
            return false;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (LABEL_HINTS.contains(trimmed) || HEADER_HINTS.contains(lower)) {
            return true;
        }
        for (String hint : LABEL_HINTS) {
            if (trimmed.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWideUniformSpread(int populatedCount, int sheetColumns, int mergeSpan) {
        if (mergeSpan >= 2) {
            return true;
        }
        if (populatedCount >= 3) {
            return true;
        }
        return sheetColumns >= 4 && populatedCount * 2 >= sheetColumns;
    }

    private static boolean isLayoutRow(List<String> values, int populatedCount) {
        if (populatedCount > 3) {
            return false;
        }
        String longest = longestNonBlank(values);
        if (longest == null) {
            return false;
        }
        if (populatedCount == 1 && longest.length() >= 6) {
            return true;
        }
        return populatedCount <= 2 && longest.length() >= 10;
    }

    private static boolean isSeparatorRow(List<String> values, int populatedCount) {
        if (populatedCount > 3) {
            return false;
        }
        if (countLabelValuePairs(values) >= 2) {
            return false;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (DATE_RANGE.matcher(value.trim()).find()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFormKvRow(List<String> values, int populatedCount, List<String> nextRowValues) {
        if (populatedCount < 2) {
            return false;
        }
        int pairs = countLabelValuePairs(values);
        if (pairs >= 2) {
            return true;
        }
        return pairs == 1 && populatedCount <= 8 && !isHeaderRow(values, nextRowValues, populatedCount);
    }

    private static int countLabelValuePairs(List<String> values) {
        int pairs = 0;
        int index = 0;
        while (index < values.size()) {
            while (index < values.size() && isBlank(values.get(index))) {
                index++;
            }
            if (index >= values.size()) {
                break;
            }
            String label = values.get(index);
            if (!isLikelyLabel(label)) {
                index++;
                continue;
            }
            int valueIndex = findNextNonBlankIndex(values, index + 1, 4);
            if (valueIndex < 0) {
                index++;
                continue;
            }
            String value = values.get(valueIndex);
            if (!isBlank(value) && !isLikelyLabel(value)) {
                pairs++;
                index = valueIndex + 1;
            } else {
                index++;
            }
        }
        return pairs;
    }

    private static boolean isHeaderRow(List<String> values, List<String> nextRowValues, int populatedCount) {
        if (populatedCount < 3) {
            return false;
        }
        int headerLike = 0;
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String trimmed = value.trim();
            if (isHeaderLikeCell(trimmed)) {
                headerLike++;
            }
        }
        if (headerLike < Math.max(2, populatedCount / 2)) {
            return false;
        }
        if (nextRowValues == null || nextRowValues.stream().allMatch(AdaptiveTableLayoutAnalyzer::isBlank)) {
            return true;
        }
        return looksLikeDataRow(nextRowValues);
    }

    private static boolean isHeaderLikeCell(String text) {
        if (text.length() > 24) {
            return false;
        }
        if (text.matches(".*[A-Za-z].*") && containsCjk(text)) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (HEADER_HINTS.contains(lower) || LABEL_HINTS.contains(text.trim())) {
            return true;
        }
        for (String hint : HEADER_HINTS) {
            if (hint.length() >= 3 && lower.contains(hint)) {
                return true;
            }
            if (hint.length() < 3 && lower.equals(hint)) {
                return true;
            }
        }
        return containsCjk(text) && !MOSTLY_NUMERIC.matcher(text).matches() && text.length() <= 6;
    }

    private static boolean looksLikeDataRow(List<String> values) {
        int populated = countPopulated(values);
        if (populated == 0) {
            return false;
        }
        String first = firstNonBlank(values);
        if (first != null && MOSTLY_NUMERIC.matcher(first.trim()).matches()) {
            return true;
        }
        int headerLike = 0;
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (isHeaderLikeCell(value.trim())) {
                headerLike++;
            }
        }
        if (populated >= 2 && headerLike < populated) {
            return true;
        }
        int longValues = 0;
        for (String value : values) {
            if (value != null && value.trim().length() >= 8) {
                longValues++;
            }
        }
        return longValues >= 1;
    }

    private static boolean isDataRow(
            List<String> values,
            String[] activeColumnHeaders,
            int populatedCount,
            RowLayoutContext layoutContext
    ) {
        if (populatedCount == 0 || activeColumnHeaders == null) {
            return false;
        }
        if (isWideUniformLayoutRow(values, List.of(), activeColumnHeaders, populatedCount, layoutContext)) {
            return false;
        }
        int aligned = 0;
        for (int index = 0; index < Math.min(values.size(), activeColumnHeaders.length); index++) {
            String header = activeColumnHeaders[index];
            String value = values.get(index);
            if (header != null && !header.isBlank() && value != null && !value.isBlank()) {
                aligned++;
            }
        }
        if (aligned >= 2) {
            return true;
        }
        String first = firstNonBlank(values);
        return first != null && MOSTLY_NUMERIC.matcher(first.trim()).matches();
    }

    private static String longestNonBlank(List<String> values) {
        String longest = null;
        for (String value : values) {
            if (isBlank(value)) {
                continue;
            }
            String trimmed = value.trim();
            if (longest == null || trimmed.length() > longest.length()) {
                longest = trimmed;
            }
        }
        return longest;
    }

    private static int findNextNonBlankIndex(List<String> values, int start, int maxLookahead) {
        int limit = Math.min(values.size(), start + maxLookahead);
        for (int index = start; index < limit; index++) {
            if (!isBlank(values.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private static int countPopulated(List<String> values) {
        int count = 0;
        for (String value : values) {
            if (!isBlank(value)) {
                count++;
            }
        }
        return count;
    }

    private static String firstNonBlank(List<String> values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean containsCjk(String text) {
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (ch >= '\u4e00' && ch <= '\u9fff') {
                return true;
            }
        }
        return false;
    }
}
