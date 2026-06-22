package com.knowbase.ingestion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses structured OCR engine output such as hOCR into layout blocks.
 */
final class OcrEngineOutputParser {

    private static final Pattern LINE = Pattern.compile(
            "(?is)<[^>]*class=[\"'][^\"']*ocr_line[^\"']*[\"'][^>]*title=[\"']([^\"']*)[\"'][^>]*>(.*?)</[^>]+>"
    );
    private static final Pattern BBOX = Pattern.compile("bbox\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");
    private static final Pattern CONFIDENCE = Pattern.compile("x_wconf\\s+(\\d+(?:\\.\\d+)?)");

    private OcrEngineOutputParser() {
    }

    static List<StructuralBlock> parseHocr(String hocr, Map<String, Object> metadata) {
        if (hocr == null || hocr.isBlank()) {
            return List.of();
        }
        List<StructuralBlock> blocks = new ArrayList<>();
        Matcher matcher = LINE.matcher(hocr);
        int ordinal = 0;
        while (matcher.find()) {
            String title = matcher.group(1);
            String text = stripTags(matcher.group(2)).trim();
            if (text.isBlank()) {
                continue;
            }
            Map<String, Object> blockMetadata = new HashMap<>();
            blockMetadata.put("layoutParsing", true);
            blockMetadata.put("ocrApplied", true);
            blockMetadata.put("ocrEngineOutput", "hocr");
            blockMetadata.put("ocrConfidenceSource", "hocr");
            blockMetadata.put("pageNumber", intMetadata(metadata, "pageNumber", 1));
            blockMetadata.put("readingOrder", ordinal);
            parseBbox(title).ifPresent(value -> blockMetadata.put("bbox", value));
            parseConfidence(title).ifPresent(value -> blockMetadata.put("ocrConfidence", value));
            if (!blockMetadata.containsKey("ocrConfidence")) {
                blockMetadata.put("ocrConfidence", -1d);
                blockMetadata.put("ocrConfidenceSource", "unavailable");
            }
            StructuralBlock block = tableLike(text)
                    ? new StructuralBlock("table_row", 0, text.replaceAll("\\s{2,}|\\t+", " | "), ordinal, blockMetadataWith(blockMetadata, "boundaryType", "table_row", "layoutRole", "table"))
                    : new StructuralBlock("paragraph", 0, text, ordinal, blockMetadataWith(blockMetadata, "boundaryType", "paragraph", "layoutRole", "body"));
            blocks.add(block);
            ordinal++;
        }
        return StructureParsingSupport.enrichHeadingPathsPublic(blocks);
    }

    private static java.util.Optional<List<Double>> parseBbox(String title) {
        Matcher matcher = BBOX.matcher(title == null ? "" : title);
        if (!matcher.find()) {
            return java.util.Optional.empty();
        }
        double x1 = Double.parseDouble(matcher.group(1));
        double y1 = Double.parseDouble(matcher.group(2));
        double x2 = Double.parseDouble(matcher.group(3));
        double y2 = Double.parseDouble(matcher.group(4));
        return java.util.Optional.of(List.of(x1, y1, Math.max(1d, x2 - x1), Math.max(1d, y2 - y1)));
    }

    private static java.util.Optional<Double> parseConfidence(String title) {
        Matcher matcher = CONFIDENCE.matcher(title == null ? "" : title);
        if (!matcher.find()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Double.parseDouble(matcher.group(1)) / 100d);
    }

    private static Map<String, Object> blockMetadataWith(Map<String, Object> base, Object... entries) {
        Map<String, Object> metadata = new HashMap<>(base);
        for (int index = 0; index < entries.length; index += 2) {
            metadata.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return Map.copyOf(metadata);
    }

    private static boolean tableLike(String text) {
        return text.contains("\t") || text.matches(".*\\S\\s{2,}\\S.*");
    }

    private static String stripTags(String html) {
        return html.replaceAll("(?is)<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private static int intMetadata(Map<String, Object> metadata, String key, int fallback) {
        if (metadata == null) {
            return fallback;
        }
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
