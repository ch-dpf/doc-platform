package com.knowbase.ingestion;

import java.util.ArrayList;
import java.util.List;

/**
 * Merges candidate text pieces into character-budget windows with overlap (MaxKB / LangChain style).
 */
public final class CharacterWindowChunker {

    public List<String> chunk(List<String> pieces, int maxChars, int overlapChars, int minChars) {
        if (pieces == null || pieces.isEmpty()) {
            return List.of();
        }
        if (maxChars <= 0) {
            maxChars = 500;
        }
        if (overlapChars < 0) {
            overlapChars = 0;
        }
        if (minChars <= 0) {
            minChars = 1;
        }

        List<String> windows = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (String piece : pieces) {
            if (piece == null || piece.isBlank()) {
                continue;
            }
            if (piece.length() > maxChars) {
                flush(buffer, minChars, windows);
                windows.addAll(hardSplit(piece, maxChars, overlapChars));
                continue;
            }
            if (buffer.length() + piece.length() + 2 > maxChars && buffer.length() > 0) {
                flush(buffer, minChars, windows);
                if (overlapChars > 0 && !windows.isEmpty()) {
                    String overlap = tail(windows.get(windows.size() - 1), overlapChars);
                    buffer.append(overlap);
                }
            }
            if (buffer.length() > 0) {
                buffer.append("\n\n");
            }
            buffer.append(piece.trim());
        }
        flush(buffer, minChars, windows);
        return windows;
    }

    private static void flush(StringBuilder buffer, int minChars, List<String> windows) {
        if (buffer.isEmpty()) {
            return;
        }
        String text = buffer.toString().trim();
        buffer.setLength(0);
        if (text.length() >= minChars) {
            windows.add(text);
        } else if (!windows.isEmpty()) {
            windows.set(windows.size() - 1, windows.get(windows.size() - 1) + "\n\n" + text);
        } else if (!text.isBlank()) {
            windows.add(text);
        }
    }

    private static List<String> hardSplit(String text, int maxChars, int overlapChars) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + maxChars);
            parts.add(text.substring(start, end).trim());
            if (end >= text.length()) {
                break;
            }
            start = Math.max(start + 1, end - overlapChars);
        }
        return parts.stream().filter(part -> !part.isBlank()).toList();
    }

    private static String tail(String text, int overlapChars) {
        if (text == null || text.length() <= overlapChars) {
            return text == null ? "" : text;
        }
        return text.substring(text.length() - overlapChars);
    }
}
