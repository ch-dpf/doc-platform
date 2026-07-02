package com.knowbase.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class BoundaryAdjuster {

    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[。！？.!?])\\s+");
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\R{2,}");

    public List<String> adjust(List<String> segments, boolean preserveStructureBoundary) {
        if (!preserveStructureBoundary || segments == null || segments.isEmpty()) {
            return segments == null ? List.of() : List.copyOf(segments);
        }
        List<String> adjusted = new ArrayList<>();
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            adjusted.add(trimAtNaturalBoundary(segment.trim()));
        }
        return adjusted;
    }

    private static String trimAtNaturalBoundary(String text) {
        if (text.length() <= 32) {
            return text;
        }
        int paragraphBreak = text.lastIndexOf("\n\n");
        if (paragraphBreak > text.length() / 3) {
            return text.substring(0, paragraphBreak).trim();
        }
        String[] sentences = SENTENCE_END.split(text);
        if (sentences.length <= 1) {
            return text;
        }
        StringBuilder builder = new StringBuilder();
        for (String sentence : sentences) {
            if (builder.length() + sentence.length() > text.length() * 0.85) {
                break;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(sentence.trim());
        }
        return builder.isEmpty() ? text : builder.toString().trim();
    }

    public static List<String> splitLongSegment(String segment, int maxChars) {
        if (segment == null || segment.length() <= maxChars) {
            return segment == null ? List.of() : List.of(segment);
        }
        List<String> parts = new ArrayList<>();
        String[] paragraphs = PARAGRAPH_BREAK.split(segment);
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                continue;
            }
            if (current.length() + paragraph.length() + 2 > maxChars && !current.isEmpty()) {
                parts.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(paragraph.trim());
        }
        if (!current.isEmpty()) {
            parts.add(current.toString().trim());
        }
        return parts.isEmpty() ? List.of(segment) : parts;
    }
}
