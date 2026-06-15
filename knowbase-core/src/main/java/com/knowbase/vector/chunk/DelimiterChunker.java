package com.knowbase.vector.chunk;

import java.util.ArrayList;
import java.util.List;

/** 按库级自定义分隔符切段（字面量 {@code \\n} 表示换行）。 */
public final class DelimiterChunker {

    private DelimiterChunker() {
    }

    public static List<String> splitSegments(String text, String delimiter) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String delim = normalizeDelimiter(delimiter);
        if (delim.isEmpty()) {
            return List.of(text.strip());
        }
        String[] raw = text.split(java.util.regex.Pattern.quote(delim), -1);
        List<String> segments = new ArrayList<>();
        for (String part : raw) {
            String stripped = part.strip();
            if (!stripped.isEmpty()) {
                segments.add(stripped);
            }
        }
        return segments.isEmpty() ? List.of(text.strip()) : segments;
    }

    static String normalizeDelimiter(String delimiter) {
        if (delimiter == null) {
            return "";
        }
        String trimmed = delimiter.strip();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.replace("\\n", "\n");
    }
}
