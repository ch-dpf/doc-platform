package com.knowbase.ingestion.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public final class OcrJsonParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OcrJsonParser() {
    }

    public static List<OcrLineResult> parse(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root.isArray()) {
                return parseArray(root);
            }
            JsonNode lines = root.get("lines");
            if (lines != null && lines.isArray()) {
                return parseArray(lines);
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return List.of();
    }

    private static List<OcrLineResult> parseArray(JsonNode array) {
        List<OcrLineResult> lines = new ArrayList<>();
        for (JsonNode node : array) {
            String text = node.path("text").asText("");
            if (text.isBlank()) {
                continue;
            }
            Double confidence = node.has("confidence") ? node.get("confidence").asDouble() : null;
            List<Double> bbox = readBbox(node.get("bbox"));
            String language = node.has("language") ? node.get("language").asText(null) : null;
            Double rotation = node.has("rotation") ? node.get("rotation").asDouble() : null;
            lines.add(new OcrLineResult(text, bbox, confidence, List.of(), "line", language, rotation));
        }
        return lines;
    }

    private static List<Double> readBbox(JsonNode bbox) {
        if (bbox == null || !bbox.isArray() || bbox.size() < 4) {
            return List.of();
        }
        return List.of(bbox.get(0).asDouble(), bbox.get(1).asDouble(), bbox.get(2).asDouble(), bbox.get(3).asDouble());
    }
}
