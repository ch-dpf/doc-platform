package com.knowbase.model.vision.vllm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowbase.model.vision.VisionDocumentModelClient;
import com.knowbase.model.vision.VisionDocumentPrompts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Calls a vLLM OpenAI-compatible chat completions endpoint for per-page document parsing.
 */
public final class VllmVisionDocumentModelClient implements VisionDocumentModelClient {

    private static final int MAX_REQUEST_BYTES = 1_500_000;

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
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(timeout)
                        .build(),
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
        String prompt = VisionDocumentPrompts.resolvePagePrompt(options, modelName);
        String resolvedMime = mimeType == null || mimeType.isBlank() ? "image/jpeg" : mimeType;
        try {
            byte[] bodyBytes = buildRequestBody(imageBytes, resolvedMime, prompt, options);
            HttpResponse<String> response = sendRequest(bodyBytes);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "vLLM 返回 HTTP " + response.statusCode()
                                + " (requestBytes=" + bodyBytes.length + "): "
                                + abbreviate(response.body())
                );
            }
            return extractAnswer(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("vLLM 调用被中断", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("vLLM 调用失败", exception);
        }
    }

    private HttpResponse<String> sendRequest(byte[] bodyBytes) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + chatCompletionsPath))
                .timeout(timeout)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json");
        if (!apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private byte[] buildRequestBody(
            byte[] imageBytes,
            String mimeType,
            String prompt,
            Map<String, Object> options
    ) throws IOException {
        byte[] payload = encodeRequestBody(imageBytes, mimeType, prompt, options);
        if (payload.length <= MAX_REQUEST_BYTES) {
            return payload;
        }
        throw new IllegalStateException(
                "vLLM 请求体过大: bytes=" + payload.length + ", imageBytes=" + imageBytes.length
        );
    }

    private byte[] encodeRequestBody(
            byte[] imageBytes,
            String mimeType,
            String prompt,
            Map<String, Object> options
    ) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUri = "data:" + mimeType + ";base64," + base64;
        boolean imageFirst = VisionDocumentPrompts.prefersImageBeforeText(modelName);
        ByteArrayOutputStream output = new ByteArrayOutputStream(base64.length() + 512);
        try (JsonGenerator generator = objectMapper.getFactory().createGenerator(output)) {
            generator.writeStartObject();
            generator.writeStringField("model", modelName);
            generator.writeNumberField("temperature", resolveTemperature());
            generator.writeNumberField("max_tokens", resolveMaxTokens(options));
            generator.writeFieldName("messages");
            generator.writeStartArray();
            generator.writeStartObject();
            generator.writeStringField("role", "user");
            generator.writeFieldName("content");
            generator.writeStartArray();
            if (imageFirst) {
                writeImagePart(generator, dataUri);
                writeTextPart(generator, prompt);
            } else {
                writeTextPart(generator, prompt);
                writeImagePart(generator, dataUri);
            }
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndArray();
            generator.writeEndObject();
        }
        byte[] bodyBytes = output.toByteArray();
        if (bodyBytes.length == 0) {
            throw new IllegalStateException("vLLM 请求体为空");
        }
        return bodyBytes;
    }

    private static void writeImagePart(JsonGenerator generator, String dataUri) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("type", "image_url");
        generator.writeObjectFieldStart("image_url");
        generator.writeStringField("url", dataUri);
        generator.writeEndObject();
        generator.writeEndObject();
    }

    private static void writeTextPart(JsonGenerator generator, String prompt) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("type", "text");
        generator.writeStringField("text", prompt);
        generator.writeEndObject();
    }

    private String extractAnswer(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "";
        }
        JsonNode assistantMessage = choices.get(0).path("message");
        String answer = assistantMessage.path("content").asText("");
        if (answer.isBlank()) {
            answer = assistantMessage.path("reasoning_content").asText("");
        }
        return answer.trim();
    }

    private double resolveTemperature() {
        if (VisionDocumentPrompts.isPaddleOcrVlModel(modelName)) {
            return 0.0d;
        }
        return temperature;
    }

    private int resolveMaxTokens(Map<String, Object> options) {
        if (options != null) {
            Object custom = options.get("vlMaxTokens");
            if (custom == null) {
                custom = options.get("maxCompletionTokens");
            }
            if (custom instanceof Number number) {
                return Math.max(256, number.intValue());
            }
        }
        return VisionDocumentPrompts.isPaddleOcrVlModel(modelName) ? 8192 : 4096;
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
