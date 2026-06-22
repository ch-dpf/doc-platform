package com.knowbase.model.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OllamaClient {

    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, Boolean> tokenizeSupportCache = new ConcurrentHashMap<>();

    public OllamaClient(String baseUrl, Duration timeout) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean supportsTokenize(String model) {
        return tokenizeSupportCache.computeIfAbsent(model, this::probeTokenizeSupport);
    }

    public List<Integer> tokenize(String model, String content) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("content", content);
        JsonNode response = post("/api/tokenize", body);
        JsonNode tokens = response.get("tokens");
        if (tokens == null || !tokens.isArray()) {
            throw new OllamaException("Ollama tokenize 响应缺少 tokens 字段");
        }
        List<Integer> tokenIds = new ArrayList<>(tokens.size());
        for (JsonNode token : tokens) {
            tokenIds.add(token.asInt());
        }
        return tokenIds;
    }

    public String detokenize(String model, List<Integer> tokenIds) {
        if (tokenIds.isEmpty()) {
            return "";
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        ArrayNode tokens = body.putArray("tokens");
        for (Integer tokenId : tokenIds) {
            tokens.add(tokenId);
        }
        JsonNode response = post("/api/detokenize", body);
        JsonNode content = response.get("content");
        if (content == null || content.isNull()) {
            throw new OllamaException("Ollama detokenize 响应缺少 content 字段");
        }
        return content.asText();
    }

    public List<float[]> embed(String model, List<String> inputs) {
        if (inputs.isEmpty()) {
            return List.of();
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        if (inputs.size() == 1) {
            body.put("input", inputs.getFirst());
        } else {
            ArrayNode arrayNode = body.putArray("input");
            for (String input : inputs) {
                arrayNode.add(input);
            }
        }
        JsonNode response = post("/api/embed", body);
        JsonNode embeddings = response.get("embeddings");
        if (embeddings == null || !embeddings.isArray()) {
            throw new OllamaException("Ollama embed 响应缺少 embeddings 字段");
        }
        List<float[]> vectors = new ArrayList<>(embeddings.size());
        for (JsonNode embeddingNode : embeddings) {
            vectors.add(readEmbedding(embeddingNode));
        }
        return vectors;
    }

    public OllamaChatResponse chat(String model, List<OllamaMessage> messages, Map<String, Object> options) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("stream", false);
        ArrayNode messageArray = body.putArray("messages");
        for (OllamaMessage message : messages) {
            ObjectNode messageNode = messageArray.addObject();
            messageNode.put("role", message.role());
            messageNode.put("content", message.content());
        }
        if (options != null && !options.isEmpty()) {
            ObjectNode optionsNode = body.putObject("options");
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                writeOption(optionsNode, entry.getKey(), entry.getValue());
            }
        }
        JsonNode response = post("/api/chat", body);
        JsonNode message = response.get("message");
        if (message == null || message.get("content") == null) {
            throw new OllamaException("Ollama chat 响应缺少 message.content 字段");
        }
        int promptTokens = response.path("prompt_eval_count").asInt(0);
        int completionTokens = response.path("eval_count").asInt(0);
        return new OllamaChatResponse(
                message.get("content").asText(),
                promptTokens,
                completionTokens,
                response.toString()
        );
    }

    private static float[] readEmbedding(JsonNode embeddingNode) {
        if (!embeddingNode.isArray()) {
            throw new OllamaException("Ollama embedding 格式不正确");
        }
        float[] vector = new float[embeddingNode.size()];
        for (int index = 0; index < embeddingNode.size(); index++) {
            vector[index] = (float) embeddingNode.get(index).asDouble();
        }
        return vector;
    }

    private void writeOption(ObjectNode optionsNode, String key, Object value) {
        if (value instanceof Number number) {
            optionsNode.put(key, number.doubleValue());
        } else if (value instanceof Boolean bool) {
            optionsNode.put(key, bool);
        } else {
            optionsNode.put(key, String.valueOf(value));
        }
    }

    private boolean probeTokenizeSupport(String model) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("content", "ping");
        try {
            post("/api/tokenize", body);
            return true;
        } catch (OllamaException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("HTTP 404")) {
                return false;
            }
            throw exception;
        }
    }

    private JsonNode post(String path, ObjectNode body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new OllamaException("Ollama 请求失败: HTTP " + response.statusCode() + " " + response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OllamaException("调用 Ollama 失败: " + path, exception);
        } catch (IOException exception) {
            throw new OllamaException("调用 Ollama 失败: " + path, exception);
        }
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:11434";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
