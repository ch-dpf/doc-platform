package com.knowbase.ingestion.ocr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public final class OcrTsvParser {

    private OcrTsvParser() {
    }

    public static List<OcrLineResult> parse(String tsv) {
        if (tsv == null || tsv.isBlank()) {
            return List.of();
        }
        List<TsvEntry> entries = parseEntries(tsv);
        if (entries.isEmpty()) {
            return List.of();
        }
        Map<String, List<TsvEntry>> wordsByLine = new LinkedHashMap<>();
        Map<String, TsvEntry> lineByKey = new HashMap<>();
        for (TsvEntry entry : entries) {
            if (entry.level == 5 && !entry.text.isBlank()) {
                wordsByLine.computeIfAbsent(lineKey(entry), ignored -> new ArrayList<>()).add(entry);
                continue;
            }
            if (entry.level == 4) {
                lineByKey.putIfAbsent(lineKey(entry), entry);
            }
        }
        if (wordsByLine.isEmpty() && lineByKey.isEmpty()) {
            return List.of();
        }
        List<String> keys = new ArrayList<>(wordsByLine.keySet());
        for (String key : lineByKey.keySet()) {
            if (!keys.contains(key)) {
                keys.add(key);
            }
        }
        List<OcrLineResult> lines = new ArrayList<>();
        for (String key : keys) {
            List<TsvEntry> words = wordsByLine.getOrDefault(key, List.of());
            TsvEntry lineEntry = lineByKey.get(key);
            if (!words.isEmpty()) {
                lines.add(toLineResult(words, lineEntry));
            } else if (lineEntry != null && !lineEntry.text.isBlank()) {
                lines.add(toLineResult(List.of(lineEntry), lineEntry));
            }
        }
        return List.copyOf(lines);
    }

    private static List<TsvEntry> parseEntries(String tsv) {
        List<TsvEntry> entries = new ArrayList<>();
        String[] rows = tsv.replace("\r\n", "\n").split("\n");
        for (int index = 1; index < rows.length; index++) {
            String row = rows[index];
            if (row.isBlank()) {
                continue;
            }
            String[] parts = row.split("\t", -1);
            if (parts.length < 12) {
                continue;
            }
            try {
                int level = Integer.parseInt(parts[0].trim());
                if (level != 4 && level != 5) {
                    continue;
                }
                entries.add(new TsvEntry(
                        level,
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()),
                        Integer.parseInt(parts[3].trim()),
                        Integer.parseInt(parts[4].trim()),
                        Integer.parseInt(parts[5].trim()),
                        Float.parseFloat(parts[6]),
                        Float.parseFloat(parts[7]),
                        Float.parseFloat(parts[8]),
                        Float.parseFloat(parts[9]),
                        Double.parseDouble(parts[10]) / 100d,
                        unescape(parts[11])
                ));
            } catch (NumberFormatException ignored) {
                // skip malformed row
            }
        }
        return entries;
    }

    private static OcrLineResult toLineResult(List<TsvEntry> words, TsvEntry lineEntry) {
        List<OcrWordResult> wordResults = new ArrayList<>();
        StringJoiner textJoiner = new StringJoiner(" ");
        double confidenceTotal = 0d;
        int confidenceCount = 0;
        for (TsvEntry word : words) {
            if (word.text.isBlank()) {
                continue;
            }
            textJoiner.add(word.text);
            wordResults.add(new OcrWordResult(
                    word.text,
                    List.of((double) word.left, (double) word.top, (double) word.width, (double) word.height),
                    word.confidence
            ));
            if (word.confidence >= 0d) {
                confidenceTotal += word.confidence;
                confidenceCount++;
            }
        }
        String text = textJoiner.toString();
        if (text.isBlank() && lineEntry != null) {
            text = lineEntry.text;
        }
        List<Double> bbox = unionBbox(words.isEmpty() && lineEntry != null ? List.of(lineEntry) : words);
        Double confidence = confidenceCount == 0
                ? lineEntry == null ? null : lineEntry.confidence
                : confidenceTotal / confidenceCount;
        return new OcrLineResult(text, bbox, confidence, List.copyOf(wordResults), "line");
    }

    private static List<Double> unionBbox(List<TsvEntry> entries) {
        if (entries.isEmpty()) {
            return List.of();
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        for (TsvEntry entry : entries) {
            minX = Math.min(minX, entry.left);
            minY = Math.min(minY, entry.top);
            maxX = Math.max(maxX, entry.left + entry.width);
            maxY = Math.max(maxY, entry.top + entry.height);
        }
        return List.of((double) minX, (double) minY, (double) (maxX - minX), (double) (maxY - minY));
    }

    private static String lineKey(TsvEntry entry) {
        return String.format(
                Locale.ROOT,
                "%d-%d-%d-%d",
                entry.page,
                entry.block,
                entry.paragraph,
                entry.line
        );
    }

    private static String unescape(String value) {
        return value == null ? "" : value.trim();
    }

    private record TsvEntry(
            int level,
            int page,
            int block,
            int paragraph,
            int line,
            int word,
            float left,
            float top,
            float width,
            float height,
            double confidence,
            String text
    ) {
    }
}
