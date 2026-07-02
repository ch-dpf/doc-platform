package com.knowbase.ingestion.ocr;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.StructureParsingSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OcrBlockFactory {

    private OcrBlockFactory() {
    }

    public static List<StructuralBlock> fromEngineResult(OcrEngineResult result, Map<String, Object> baseMetadata) {
        if (result == null) {
            return List.of();
        }
        if ("hocr".equalsIgnoreCase(result.rawFormat()) && result.rawPayload() != null) {
            return fromHocr(result.rawPayload(), baseMetadata);
        }
        if ("tsv".equalsIgnoreCase(result.rawFormat())) {
            return fromLines(OcrTsvParser.parse(result.rawPayload()), baseMetadata, "tsv");
        }
        if ("json".equalsIgnoreCase(result.rawFormat())) {
            return fromLines(OcrJsonParser.parse(result.rawPayload()), baseMetadata, "json");
        }
        return fromPlainText(result.rawPayload(), baseMetadata);
    }

    public static List<StructuralBlock> fromHocr(String hocr, Map<String, Object> metadata) {
        List<StructuralBlock> blocks = OcrHocrParser.parse(hocr, metadata);
        if (!blocks.isEmpty()) {
            return blocks;
        }
        return List.of();
    }

    public static List<StructuralBlock> fromPlainText(String text, Map<String, Object> metadata) {
        List<OcrLineResult> lines = OcrPlainTextParser.parse(text);
        if (lines.isEmpty()) {
            return StructureParsingSupport.parseOcrLayout(text, metadata);
        }
        return fromLines(lines, metadata, "text");
    }

    private static List<StructuralBlock> fromLines(
            List<OcrLineResult> lines,
            Map<String, Object> baseMetadata,
            String source
    ) {
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        int pageNumber = intMetadata(baseMetadata, "pageNumber", 1);
        for (OcrLineResult line : lines) {
            Map<String, Object> blockMetadata = new HashMap<>(baseMetadata == null ? Map.of() : baseMetadata);
            blockMetadata.put("layoutParsing", true);
            blockMetadata.put("ocrApplied", true);
            blockMetadata.put("ocrEngineOutput", source);
            blockMetadata.put("ocrLevel", line.level() == null ? "line" : line.level());
            blockMetadata.put("pageNumber", pageNumber);
            blockMetadata.put("readingOrder", ordinal);
            if (line.bbox() != null && line.bbox().size() == 4) {
                blockMetadata.put("bbox", line.bbox());
                blockMetadata.put("bboxSource", "engine");
            } else {
                blockMetadata.put("bboxSource", "unavailable");
            }
            if (line.confidence() != null) {
                blockMetadata.put("ocrConfidence", line.confidence());
                blockMetadata.put("ocrConfidenceSource", source);
            } else {
                blockMetadata.put("ocrConfidence", -1d);
                blockMetadata.put("ocrConfidenceSource", "unavailable");
            }
            if (line.language() != null && !line.language().isBlank()) {
                blockMetadata.put("ocrLanguage", line.language());
            } else if (baseMetadata != null && baseMetadata.get("ocrLanguage") != null) {
                blockMetadata.put("ocrLanguage", baseMetadata.get("ocrLanguage"));
            }
            if (line.rotation() != null) {
                blockMetadata.put("rotation", line.rotation());
            }
            if (line.words() != null && !line.words().isEmpty()) {
                blockMetadata.put("ocrWords", toWordMetadata(line.words()));
            }
            boolean tableLike = line.text().contains("\t") || line.text().matches(".*\\S\\s{2,}\\S.*");
            StructuralBlock block = tableLike
                    ? new StructuralBlock(
                    "table_row",
                    0,
                    line.text().replaceAll("\\s{2,}|\\t+", " | "),
                    ordinal,
                    enrich(blockMetadata, "boundaryType", "table_row", "layoutRole", "table")
            )
                    : new StructuralBlock(
                    "paragraph",
                    0,
                    line.text(),
                    ordinal,
                    enrich(blockMetadata, "boundaryType", "paragraph", "layoutRole", "body")
            );
            blocks.add(block);
            ordinal++;
        }
        return StructureParsingSupport.enrichHeadingPathsPublic(blocks);
    }

    private static Map<String, Object> enrich(Map<String, Object> base, Object... entries) {
        Map<String, Object> metadata = new HashMap<>(base);
        for (int index = 0; index < entries.length; index += 2) {
            metadata.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return Map.copyOf(metadata);
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

    private static List<Map<String, Object>> toWordMetadata(List<OcrWordResult> words) {
        List<Map<String, Object>> metadata = new ArrayList<>(words.size());
        for (OcrWordResult word : words) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("text", word.text());
            if (word.bbox() != null && !word.bbox().isEmpty()) {
                entry.put("bbox", word.bbox());
            }
            if (word.confidence() != null) {
                entry.put("confidence", word.confidence());
            }
            metadata.add(Map.copyOf(entry));
        }
        return List.copyOf(metadata);
    }
}
