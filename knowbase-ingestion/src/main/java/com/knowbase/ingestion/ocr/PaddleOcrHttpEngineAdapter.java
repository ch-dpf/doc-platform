package com.knowbase.ingestion.ocr;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP adapter for a PaddleOCR-style service returning JSON lines compatible with {@link OcrJsonParser}.
 * Configure via {@code paddleOcrEndpoint} option or {@code KNOWBASE_PADDLE_OCR_ENDPOINT} env.
 */
public final class PaddleOcrHttpEngineAdapter implements OcrEngineAdapter {

    private final HttpClient httpClient;
    private final String defaultEndpoint;

    public PaddleOcrHttpEngineAdapter() {
        this(HttpClient.newHttpClient(), System.getenv("KNOWBASE_PADDLE_OCR_ENDPOINT"));
    }

    PaddleOcrHttpEngineAdapter(HttpClient httpClient, String defaultEndpoint) {
        this.httpClient = httpClient;
        this.defaultEndpoint = defaultEndpoint;
    }

    @Override
    public String engineCode() {
        return "paddle";
    }

    @Override
    public boolean supports(String mimeType, Map<String, Object> options) {
        return resolveEndpoint(options) != null;
    }

    @Override
    public OcrEngineResult recognize(byte[] content, OcrRecognizeRequest request) {
        String endpoint = resolveEndpoint(request.options());
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Paddle OCR endpoint 未配置（paddleOcrEndpoint 或 KNOWBASE_PADDLE_OCR_ENDPOINT）");
        }
        try {
            String mimeType = request.mimeType() == null || request.mimeType().isBlank()
                    ? "application/octet-stream"
                    : request.mimeType();
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Content-Type", mimeType)
                    .header("X-KnowBase-Source-Uri", request.sourceUri() == null ? "" : request.sourceUri())
                    .header("X-KnowBase-Ocr-Engine", engineCode())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Paddle OCR 返回状态码: " + response.statusCode());
            }
            Map<String, Object> engineMetadata = new HashMap<>();
            engineMetadata.put("ocrEngine", engineCode());
            engineMetadata.put("paddleOcrEndpoint", endpoint);
            engineMetadata.put("paddleOcrStatus", response.statusCode());
            return new OcrEngineResult(
                    engineCode(),
                    "json",
                    response.body(),
                    List.of(),
                    Map.copyOf(engineMetadata)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Paddle OCR 调用失败: " + request.sourceUri(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Paddle OCR 调用被中断: " + request.sourceUri(), exception);
        }
    }

    private String resolveEndpoint(Map<String, Object> options) {
        if (options != null) {
            Object endpoint = options.get("paddleOcrEndpoint");
            if (endpoint == null) {
                endpoint = options.get("ocrEndpoint");
            }
            if (endpoint != null && !String.valueOf(endpoint).isBlank()) {
                return String.valueOf(endpoint).trim();
            }
        }
        return defaultEndpoint == null || defaultEndpoint.isBlank() ? null : defaultEndpoint.trim();
    }
}
