package com.knowbase.ingestion.table;

import com.knowbase.ingestion.adaptive.TableRowRole;

import java.util.List;

/**
 * Detects region breaks inside one sheet when layout/separator rows split multiple tables.
 */
public final class AdaptiveTableRegionDetector {

    private AdaptiveTableRegionDetector() {
    }

    public static boolean shouldStartRegion(TableRowRole role, List<String> values, int populatedCount) {
        if (role == TableRowRole.LAYOUT) {
            return true;
        }
        if (role == TableRowRole.SEPARATOR) {
            return false;
        }
        return false;
    }

    public static String regionLabel(TableRowRole role, List<String> values) {
        if (role != TableRowRole.LAYOUT) {
            return "";
        }
        String uniform = uniformPopulatedValue(values);
        if (uniform != null && !uniform.isBlank()) {
            return uniform;
        }
        return joinNonBlank(values, " ");
    }

    private static String uniformPopulatedValue(List<String> values) {
        String uniform = null;
        int populated = 0;
        for (String value : values) {
            if (value == null || value.isBlank()) {
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

    private static String joinNonBlank(List<String> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(delimiter);
                }
                builder.append(value.trim());
            }
        }
        return builder.toString();
    }
}
