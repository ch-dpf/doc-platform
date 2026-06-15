package com.knowbase.ingest.support;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 按段落去重：以换行（含单个换行）切分段落，保留首次出现顺序，段落间以空行重连。
 */
public final class DuplicateParagraphCleaner {

    private static final Pattern PARAGRAPH_SPLIT = Pattern.compile("(?:\\n\\s*)+");

    private DuplicateParagraphCleaner() {
    }

    public static String removeDuplicates(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text.strip();
        }
        String[] raw = PARAGRAPH_SPLIT.split(text.strip());
        Set<String> seen = new HashSet<>();
        List<String> unique = new ArrayList<>();
        for (String part : raw) {
            String paragraph = part.strip();
            if (paragraph.isEmpty()) {
                continue;
            }
            if (seen.add(paragraph)) {
                unique.add(paragraph);
            }
        }
        if (unique.isEmpty()) {
            return "";
        }
        return String.join("\n\n", unique);
    }
}
