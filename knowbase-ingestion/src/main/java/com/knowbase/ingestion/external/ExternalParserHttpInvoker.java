package com.knowbase.ingestion.external;

import com.knowbase.ingestion.DocumentSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * HTTP invocation with retry, auth, and timing for external parser endpoints.
 */
public final class ExternalParserHttpInvoker {

    public record InvokeResult(int statusCode, String body, long durationMs, int attempts) {
    }

    private ExternalParserHttpInvoker() {
    }

    public static InvokeResult invoke(
            HttpClient httpClient,
            String endpoint,
            byte[] rawContent,
            DocumentSource source,
            String parserCode,
            Map<String, Object> metadata,
            ExternalParserClientOptions options
    ) throws IOException, InterruptedException {
        long startedAt = System.nanoTime();
        IOException lastIo = null;
        int attempts = 0;
        for (int attempt = 1; attempt <= options.maxAttempts(); attempt++) {
            attempts = attempt;
            try {
                HttpRequest request = buildRequest(endpoint, rawContent, source, parserCode, metadata, options);
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (isRetryableStatus(response.statusCode()) && attempt < options.maxAttempts()) {
                    continue;
                }
                long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
                return new InvokeResult(response.statusCode(), response.body(), durationMs, attempts);
            } catch (IOException exception) {
                lastIo = exception;
                if (attempt >= options.maxAttempts()) {
                    throw exception;
                }
            }
        }
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
        if (lastIo != null) {
            throw lastIo;
        }
        return new InvokeResult(503, "", durationMs, attempts);
    }

    private static HttpRequest buildRequest(
            String endpoint,
            byte[] rawContent,
            DocumentSource source,
            String parserCode,
            Map<String, Object> metadata,
            ExternalParserClientOptions options
    ) {
        byte[] body = options.useJsonRequest()
                ? ExternalParserRequestBuilder.buildJsonBody(source, rawContent, parserCode, metadata)
                : rawContent;
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint.trim()))
                .timeout(options.timeout())
                .header("Accept", "application/json")
                .header("X-KnowBase-Source-Uri", source.sourceUri() == null ? "" : source.sourceUri())
                .header("X-KnowBase-Parser", parserCode == null ? "external" : parserCode)
                .header("X-KnowBase-Schema-Version", ExternalParserResponseMapper.SCHEMA_VERSION)
                .header("X-KnowBase-Request-Schema-Version", ExternalParserRequestBuilder.REQUEST_SCHEMA_VERSION);
        if (options.authBearerToken() != null && !options.authBearerToken().isBlank()) {
            builder.header("Authorization", "Bearer " + options.authBearerToken().trim());
        }
        if (options.useJsonRequest()) {
            builder.header("Content-Type", "application/json");
        } else {
            builder.header("Content-Type", source.mimeType() == null || source.mimeType().isBlank()
                    ? "application/octet-stream"
                    : source.mimeType());
        }
        return builder.POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
    }

    private static boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }
}
