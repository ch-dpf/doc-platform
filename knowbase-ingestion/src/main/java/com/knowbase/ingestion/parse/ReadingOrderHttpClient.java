package com.knowbase.ingestion.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.knowbase.ingestion.StructuralBlock;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP adapter for dedicated reading-order models (PP-DocLayout pointer network, etc.).
 */
public final class ReadingOrderHttpClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    public ReadingOrderHttpClient() {
        this(HttpClient.newHttpClient(), new ObjectMapper(), Duration.ofSeconds(30));
    }

    ReadingOrderHttpClient(HttpClient httpClient, ObjectMapper objectMapper, Duration timeout) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
    }

    public List<StructuralBlock> order(String endpoint, List<StructuralBlock> blocks) {
        if (endpoint == null || endpoint.isBlank() || blocks == null || blocks.isEmpty()) {
            return null;
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            ArrayNode blockArray = payload.putArray("blocks");
            for (int index = 0; index < blocks.size(); index++) {
                StructuralBlock block = blocks.get(index);
                ObjectNode item = blockArray.addObject();
                item.put("index", index);
                item.put("type", block.blockType());
                item.put("content", block.content());
                Object pageNumber = block.metadata().get("pageNumber");
                if (pageNumber instanceof Number number) {
                    item.put("pageNumber", number.intValue());
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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint.trim()))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            return applyResponse(objectMapper.readTree(response.body()), blocks);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException exception) {
            return null;
        }
    }

    private List<StructuralBlock> applyResponse(JsonNode root, List<StructuralBlock> blocks) {
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
        if (orderByIndex.isEmpty()) {
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
            metadata.put("readingOrderSource", "remote-http");
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
    }
}
