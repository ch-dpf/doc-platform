package com.knowbase.model.vision.paddleocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.knowbase.model.vision.VisionDocumentModelClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Calls the official PaddleOCR-VL HTTP service ({@code POST /layout-parsing}).
 */
public final class PaddleOcrVlVisionDocumentModelClient implements VisionDocumentModelClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String layoutParsingPath;
    private final Duration timeout;
    private final String pipelineName;
    private final boolean prettifyMarkdown;
    private final boolean returnMarkdownImages;
    private final Boolean visualize;

    public PaddleOcrVlVisionDocumentModelClient(
            String baseUrl,
            String layoutParsingPath,
            Duration timeout,
            String pipelineName,
            boolean prettifyMarkdown,
            boolean returnMarkdownImages,
            Boolean visualize
    ) {
        this(
                HttpClient.newBuilder().connectTimeout(timeout).build(),
                new ObjectMapper(),
                baseUrl,
                layoutParsingPath,
                timeout,
                pipelineName,
                prettifyMarkdown,
                returnMarkdownImages,
                visualize
        );
    }

    PaddleOcrVlVisionDocumentModelClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            String baseUrl,
            String layoutParsingPath,
            Duration timeout,
            String pipelineName,
            boolean prettifyMarkdown,
            boolean returnMarkdownImages,
            Boolean visualize
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.layoutParsingPath = normalizePath(layoutParsingPath);
        this.timeout = timeout;
        this.pipelineName = pipelineName == null || pipelineName.isBlank()
                ? "PaddleOCR-VL"
                : pipelineName.trim();
        this.prettifyMarkdown = prettifyMarkdown;
        this.returnMarkdownImages = returnMarkdownImages;
        this.visualize = visualize;
    }

    @Override
    public String modelName() {
        return pipelineName;
    }

    @Override
    public String recognizePage(byte[] imageBytes, String mimeType, int pageNumber, Map<String, Object> options) {
        if (imageBytes == null || imageBytes.length == 0) {
            return "";
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("file", Base64.getEncoder().encodeToString(imageBytes));
            payload.put("fileType", 1);
            payload.put("prettifyMarkdown", prettifyMarkdown);
            payload.put("returnMarkdownImages", returnMarkdownImages);
            payload.put("restructurePages", false);
            if (visualize != null) {
                payload.put("visualize", visualize);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + layoutParsingPath))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "PaddleOCR-VL 返回 HTTP " + response.statusCode() + ": " + abbreviate(response.body())
                );
            }
            JsonNode root = objectMapper.readTree(response.body());
            int errorCode = root.path("errorCode").asInt(response.statusCode());
            if (errorCode != 0) {
                throw new IllegalStateException(
                        "PaddleOCR-VL 解析失败: " + root.path("errorMsg").asText("unknown error")
                );
            }
            JsonNode results = root.path("result").path("layoutParsingResults");
            if (!results.isArray() || results.isEmpty()) {
                return "";
            }
            String markdown = results.get(0).path("markdown").path("text").asText("");
            return markdown.trim();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("PaddleOCR-VL 调用被中断", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("PaddleOCR-VL 调用失败", exception);
        }
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8080";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/layout-parsing";
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
