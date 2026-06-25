package com.knowbase.model.vision.vllm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.knowbase.model.vision.VisionDocumentModelClient;
import com.knowbase.model.vision.VisionDocumentPrompts;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Calls a vLLM OpenAI-compatible chat completions endpoint for per-page document parsing.
 */
public final class VllmVisionDocumentModelClient implements VisionDocumentModelClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String chatCompletionsPath;
    private final Duration timeout;
    private final String modelName;
    private final String apiKey;
    private final double temperature;

    public VllmVisionDocumentModelClient(
            String baseUrl,
            String chatCompletionsPath,
            Duration timeout,
            String modelName,
            String apiKey,
            double temperature
    ) {
        this(
                HttpClient.newBuilder().connectTimeout(timeout).build(),
                new ObjectMapper(),
                baseUrl,
                chatCompletionsPath,
                timeout,
                modelName,
                apiKey,
                temperature
        );
    }

    VllmVisionDocumentModelClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            String baseUrl,
            String chatCompletionsPath,
            Duration timeout,
            String modelName,
            String apiKey,
            double temperature
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.chatCompletionsPath = normalizePath(chatCompletionsPath);
        this.timeout = timeout;
        this.modelName = modelName == null || modelName.isBlank()
                ? "PaddleOCR-VL-1.6-0.9B"
                : modelName.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.temperature = temperature;
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public String recognizePage(byte[] imageBytes, String mimeType, int pageNumber, Map<String, Object> options) {
        if (imageBytes == null || imageBytes.length == 0) {
            return "";
        }
        String prompt = VisionDocumentPrompts.resolvePagePrompt(options);
        String resolvedMime = mimeType == null || mimeType.isBlank() ? "image/png" : mimeType;
        String dataUri = "data:" + resolvedMime + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", modelName);
            payload.put("temperature", temperature);
            ArrayNode messages = payload.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            ArrayNode content = message.putArray("content");
            content.addObject().put("type", "text").put("text", prompt);
            content.addObject()
                    .put("type", "image_url")
                    .putObject("image_url")
                    .put("url", dataUri);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + chatCompletionsPath))
                    .timeout(timeout)
                    .header("Content-Type", "application/json");
            if (!apiKey.isBlank()) {
                builder.header("Authorization", "Bearer " + apiKey);
            }
            HttpRequest request = builder
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "vLLM 返回 HTTP " + response.statusCode() + ": " + abbreviate(response.body())
                );
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return "";
            }
            String answer = choices.get(0).path("message").path("content").asText("");
            return answer.trim();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("vLLM 调用被中断", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("vLLM 调用失败", exception);
        }
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8118";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/v1/chat/completions";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 240 ? body : body.substring(0, 240) + "...";
    }
}
