package com.knowbase.ingestion;

import java.util.ArrayList;
import java.util.List;

/**
 * LangChain-style recursive character splitter: try coarse separators first, then fall back.
 */
public final class RecursiveCharacterSplitter {

    public RecursiveCharacterSplitter() {
    }

    public static List<String> defaultSeparators() {
        return List.of(
                "\n\n",
                "\n",
                "。",
                "！",
                "？",
                "；",
                ". ",
                "! ",
                "? ",
                "; ",
                " ",
                ""
        );
    }

    /**
     * Split text into overlapping character windows after recursive boundary split.
     */
    public List<String> splitToWindows(String text, List<String> separators, int maxChars, int overlapChars) {
        List<String> pieces = split(text, separators, maxChars);
        if (pieces.isEmpty()) {
            return List.of();
        }
        if (overlapChars <= 0 || pieces.size() == 1) {
            return pieces;
        }
        CharacterWindowChunker windowChunker = new CharacterWindowChunker();
        return windowChunker.chunk(pieces, maxChars, overlapChars, Math.min(80, maxChars / 4));
    }

    public List<String> split(String text, List<String> separators, int maxChars) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        if (maxChars <= 0) {
            maxChars = Integer.MAX_VALUE;
        }
        List<String> effectiveSeparators = separators == null || separators.isEmpty()
                ? defaultSeparators()
                : separators;
        return splitRecursive(text.trim(), effectiveSeparators, 0, maxChars);
    }

    private List<String> splitRecursive(String text, List<String> separators, int separatorIndex, int maxChars) {
        if (text.length() <= maxChars) {
            return List.of(text);
        }
        if (separatorIndex >= separators.size()) {
            return hardSplit(text, maxChars);
        }

        String separator = separators.get(separatorIndex);
        if (separator.isEmpty()) {
            return hardSplit(text, maxChars);
        }

        List<String> parts = splitKeepSeparator(text, separator);
        if (parts.size() <= 1) {
            return splitRecursive(text, separators, separatorIndex + 1, maxChars);
        }

        List<String> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (current.length() + part.length() > maxChars && current.length() > 0) {
                merged.addAll(finishPart(current.toString(), separators, separatorIndex, maxChars));
                current = new StringBuilder();
            }
            if (part.length() > maxChars) {
                if (current.length() > 0) {
                    merged.addAll(finishPart(current.toString(), separators, separatorIndex, maxChars));
                    current = new StringBuilder();
                }
                merged.addAll(splitRecursive(part, separators, separatorIndex + 1, maxChars));
                continue;
            }
            current.append(part);
        }
        if (current.length() > 0) {
            merged.addAll(finishPart(current.toString(), separators, separatorIndex, maxChars));
        }
        return merged;
    }

    private List<String> finishPart(String text, List<String> separators, int separatorIndex, int maxChars) {
        String trimmed = text.trim();
        if (trimmed.length() <= maxChars) {
            return List.of(trimmed);
        }
        return splitRecursive(trimmed, separators, separatorIndex + 1, maxChars);
    }

    private static List<String> splitKeepSeparator(String text, String separator) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int index = text.indexOf(separator);
        while (index >= 0) {
            int end = index + separator.length();
            parts.add(text.substring(start, end));
            start = end;
            index = text.indexOf(separator, start);
        }
        if (start < text.length()) {
            parts.add(text.substring(start));
        }
        return parts;
    }

    private static List<String> hardSplit(String text, int maxChars) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + maxChars);
            parts.add(text.substring(start, end).trim());
            if (end >= text.length()) {
                break;
            }
            start = end;
        }
        return parts.stream().filter(part -> !part.isBlank()).toList();
    }
}
