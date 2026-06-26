package com.knowbase.ingestion.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.layout.OllamaLayoutPrompts;
import com.knowbase.model.ollama.OllamaClient;
import com.knowbase.model.ollama.OllamaMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated reading-order model via Ollama chat ({@code knowbase-reading-order} or configured model).
 * Falls back to heuristic when unavailable (handled by {@link ReadingOrderService}).
 */
public final class OllamaReadingOrderClient {

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    public OllamaReadingOrderClient(OllamaClient ollamaClient, Duration timeout) {
        this(ollamaClient, new ObjectMapper(), timeout);
    }

    OllamaReadingOrderClient(OllamaClient ollamaClient, ObjectMapper objectMapper, Duration timeout) {
        this.ollamaClient = ollamaClient;
        this.objectMapper = objectMapper;
        this.timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
    }

    public List<StructuralBlock> order(String model, List<StructuralBlock> blocks) {
        if (ollamaClient == null || model == null || model.isBlank() || blocks == null || blocks.isEmpty()) {
            return null;
        }
        try {
            String blocksJson = objectMapper.writeValueAsString(buildBlockPayload(blocks));
            String prompt = OllamaLayoutPrompts.readingOrderUserPrompt(blocksJson);
            String answer = ollamaClient.chat(
                    model.trim(),
                    List.of(
                            OllamaMessage.system(OllamaLayoutPrompts.READING_ORDER_SYSTEM),
                            OllamaMessage.user(prompt)
                    ),
                    Map.of("temperature", 0.0d)
            ).answer();
            return applyResponse(extractJsonObject(answer), blocks);
        } catch (Exception exception) {
            return null;
        }
    }

    private ArrayNode buildBlockPayload(List<StructuralBlock> blocks) {
        ArrayNode array = objectMapper.createArrayNode();
        for (int index = 0; index < blocks.size(); index++) {
            StructuralBlock block = blocks.get(index);
            ObjectNode item = array.addObject();
            item.put("index", index);
            item.put("type", block.blockType());
            item.put("contentPreview", preview(block.content(), 120));
            Object pageNumber = block.metadata().get("pageNumber");
            if (pageNumber instanceof Number number) {
                item.put("pageNumber", number.intValue());
            }
            Object columnIndex = block.metadata().get("columnIndex");
            if (columnIndex instanceof Number number) {
                item.put("columnIndex", number.intValue());
            }
            Object bbox = block.metadata().get("bbox");
            if (bbox instanceof List<?> bboxList) {
                ArrayNode bboxNode = item.putArray("bbox");
                for (Object value : bboxList) {
                    if (value instanceof Number number) {
                        bboxNode.add(number.doubleValue());
                    }
                }
            }
        }
        return array;
    }

    private List<StructuralBlock> applyResponse(String json, List<StructuralBlock> blocks) {
        try {
            JsonNode root = objectMapper.readTree(json);
            Map<Integer, Integer> orderByIndex = new HashMap<>();
            JsonNode orders = root.path("orders");
            if (orders.isArray()) {
                for (JsonNode item : orders) {
                    orderByIndex.put(item.path("index").asInt(-1), item.path("readingOrder").asInt(-1));
                }
            } else if (root.path("readingOrder").isArray()) {
                int order = 0;
                for (JsonNode item : root.path("readingOrder")) {
                    orderByIndex.put(item.asInt(-1), order++);
                }
            }
            if (orderByIndex.size() != blocks.size()) {
                return null;
            }
            List<StructuralBlock> ordered = new ArrayList<>(blocks.size());
            for (int index = 0; index < blocks.size(); index++) {
                StructuralBlock block = blocks.get(index);
                Integer readingOrder = orderByIndex.get(index);
                if (readingOrder == null) {
                    return null;
                }
                Map<String, Object> metadata = new HashMap<>(block.metadata());
                metadata.put("readingOrder", readingOrder);
                metadata.put("readingOrderSource", "ollama-reading-order");
                ordered.add(new StructuralBlock(
                        block.blockType(),
                        block.level(),
                        block.content(),
                        readingOrder,
                        Map.copyOf(metadata)
                ));
            }
            ordered.sort((left, right) -> Integer.compare(
                    ((Number) left.metadata().get("readingOrder")).intValue(),
                    ((Number) right.metadata().get("readingOrder")).intValue()
            ));
            return List.copyOf(ordered);
        } catch (Exception exception) {
            return null;
        }
    }

    private static String preview(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        String trimmed = content.replace('\n', ' ').trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private static String extractJsonObject(String raw) {
        if (raw == null) {
            return "{}";
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw.trim();
    }
}
