package com.knowbase.ingestion.ocr;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.StructureParsingSupport;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OcrHocrParser {

    private OcrHocrParser() {
    }

    public static List<StructuralBlock> parse(String hocr, Map<String, Object> metadata) {
        if (hocr == null || hocr.isBlank()) {
            return List.of();
        }
        Document document = Jsoup.parse(hocr, "", Parser.xmlParser());
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        for (Element line : document.select(".ocr_line, [class~=ocr_line]")) {
            String text = line.text().trim();
            if (text.isBlank()) {
                continue;
            }
            String title = line.attr("title");
            Map<String, Object> blockMetadata = baseMetadata(metadata, ordinal);
            parseBbox(title).ifPresent(value -> {
                blockMetadata.put("bbox", value);
                blockMetadata.put("bboxSource", "engine");
            });
            parseConfidence(title).ifPresent(value -> blockMetadata.put("ocrConfidence", value));
            List<Map<String, Object>> words = parseWords(line);
            if (!words.isEmpty()) {
                blockMetadata.put("ocrWords", words);
            }
            if (!blockMetadata.containsKey("ocrConfidence")) {
                blockMetadata.put("ocrConfidence", -1d);
                blockMetadata.put("ocrConfidenceSource", "unavailable");
            }
            if (!blockMetadata.containsKey("bboxSource")) {
                blockMetadata.put("bboxSource", "unavailable");
            }
            StructuralBlock block = tableLike(text)
                    ? new StructuralBlock(
                    "table_row",
                    0,
                    text.replaceAll("\\s{2,}|\\t+", " | "),
                    ordinal,
                    blockMetadataWith(blockMetadata, "boundaryType", "table_row", "layoutRole", "table")
            )
                    : new StructuralBlock(
                    "paragraph",
                    0,
                    text,
                    ordinal,
                    blockMetadataWith(blockMetadata, "boundaryType", "paragraph", "layoutRole", "body")
            );
            blocks.add(block);
            ordinal++;
        }
        return StructureParsingSupport.enrichHeadingPathsPublic(blocks);
    }

    private static Map<String, Object> baseMetadata(Map<String, Object> metadata, int ordinal) {
        Map<String, Object> blockMetadata = new HashMap<>();
        blockMetadata.put("layoutParsing", true);
        blockMetadata.put("ocrApplied", true);
        blockMetadata.put("ocrEngineOutput", "hocr");
        blockMetadata.put("ocrConfidenceSource", "hocr");
        blockMetadata.put("ocrLevel", "line");
        blockMetadata.put("pageNumber", intMetadata(metadata, "pageNumber", 1));
        if (metadata != null && metadata.get("ocrLanguage") != null) {
            blockMetadata.put("ocrLanguage", metadata.get("ocrLanguage"));
        }
        blockMetadata.put("readingOrder", ordinal);
        return blockMetadata;
    }

    private static List<Map<String, Object>> parseWords(Element line) {
        List<Map<String, Object>> words = new ArrayList<>();
        for (Element word : line.select(".ocrx_word, .ocr_word, [class~=ocrx_word], [class~=ocr_word]")) {
            String text = word.text().trim();
            if (text.isBlank()) {
                continue;
            }
            Map<String, Object> entry = new HashMap<>();
            entry.put("text", text);
            String title = word.attr("title");
            parseBbox(title).ifPresent(value -> entry.put("bbox", value));
            parseConfidence(title).ifPresent(value -> entry.put("confidence", value));
            words.add(Map.copyOf(entry));
        }
        return List.copyOf(words);
    }

    private static Map<String, Object> blockMetadataWith(Map<String, Object> base, Object... entries) {
        Map<String, Object> metadata = new HashMap<>(base);
        for (int index = 0; index < entries.length; index += 2) {
            metadata.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return Map.copyOf(metadata);
    }

    private static java.util.Optional<List<Double>> parseBbox(String title) {
        if (title == null || title.isBlank()) {
            return java.util.Optional.empty();
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("bbox\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)")
                .matcher(title);
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
        if (title == null || title.isBlank()) {
            return java.util.Optional.empty();
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("x_wconf\\s+(\\d+(?:\\.\\d+)?)")
                .matcher(title);
        if (!matcher.find()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Double.parseDouble(matcher.group(1)) / 100d);
    }

    private static boolean tableLike(String text) {
        return text.contains("\t") || text.matches(".*\\S\\s{2,}\\S.*");
    }

    private static int intMetadata(Map<String, Object> metadata, String key, int fallback) {
        if (metadata == null) {
            return fallback;
        }
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }
}
