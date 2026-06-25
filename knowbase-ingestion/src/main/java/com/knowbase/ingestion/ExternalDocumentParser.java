package com.knowbase.ingestion;

import com.knowbase.ingestion.external.ExternalParserFallbackResolver;
import com.knowbase.ingestion.external.ExternalParserResponseMapper;
import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional adapter for external parsers such as Docling or Unstructured.
 */
public final class ExternalDocumentParser implements DocumentParser {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;
    private final String defaultEndpoint;
    private final List<DocumentParser> fallbackParsers;

    public ExternalDocumentParser() {
        this(defaultHttpClient(), System.getenv("KNOWBASE_EXTERNAL_PARSER_ENDPOINT"), List.of());
    }

    public ExternalDocumentParser(List<DocumentParser> fallbackParsers) {
        this(defaultHttpClient(), System.getenv("KNOWBASE_EXTERNAL_PARSER_ENDPOINT"), fallbackParsers);
    }

    public ExternalDocumentParser(HttpClient httpClient, String defaultEndpoint, List<DocumentParser> fallbackParsers) {
        this.httpClient = httpClient;
        this.defaultEndpoint = defaultEndpoint;
        this.fallbackParsers = fallbackParsers == null ? List.of() : List.copyOf(fallbackParsers);
    }

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        return defaultEndpoint != null && !defaultEndpoint.isBlank();
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        String endpoint = endpoint(source.metadata());
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("外部解析器未配置 endpoint");
        }
        final byte[] content;
        try {
            content = source.inputStream().readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("读取文档失败: " + source.sourceUri(), exception);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(resolveTimeout(source.metadata()))
                    .header("Content-Type", source.mimeType() == null || source.mimeType().isBlank()
                            ? "application/octet-stream"
                            : source.mimeType())
                    .header("X-KnowBase-Source-Uri", source.sourceUri() == null ? "" : source.sourceUri())
                    .header("X-KnowBase-Parser", parserCode(source.metadata()))
                    .header("X-KnowBase-Schema-Version", ExternalParserResponseMapper.SCHEMA_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("外部解析器返回状态码: " + response.statusCode());
            }
            Map<String, Object> metadata = new HashMap<>();
            if (source.metadata() != null) {
                metadata.putAll(source.metadata());
            }
            metadata.put("externalParserEndpoint", endpoint);
            metadata.put("externalParserStatus", response.statusCode());
            ParsedDocument parsed = ExternalParserResponseMapper.map(
                    source.sourceUri(),
                    firstNonBlank(source.filename(), source.sourceUri()),
                    response.body(),
                    parserCode(source.metadata()),
                    Map.copyOf(metadata)
            );
            return ParsedDocumentParseEnricher.enrich(parsed);
        } catch (IOException | RuntimeException exception) {
            return fallbackParse(source, content, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return fallbackParse(source, content, exception);
        }
    }

    private ParsedDocument fallbackParse(DocumentSource source, byte[] content, Exception cause) {
        if (!ExternalParserFallbackResolver.isFallbackEnabled(source.metadata())) {
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("外部解析器调用失败: " + source.sourceUri(), cause);
        }
        DocumentParser fallback = ExternalParserFallbackResolver.resolve(source, fallbackParsers)
                .orElseThrow(() -> cause instanceof RuntimeException runtime
                        ? runtime
                        : new IllegalStateException("外部解析器失败且无可用 fallback: " + source.sourceUri(), cause));
        Map<String, Object> metadata = new HashMap<>();
        if (source.metadata() != null) {
            metadata.putAll(source.metadata());
        }
        metadata.put("externalParserFallback", true);
        metadata.put("externalParserFallbackReason", cause.getMessage() == null
                ? cause.getClass().getSimpleName()
                : cause.getMessage());
        metadata.put("externalParserFallbackParser", fallback.getClass().getSimpleName());
        DocumentSource retrySource = new DocumentSource(
                source.sourceUri(),
                source.filename(),
                source.mimeType(),
                new java.io.ByteArrayInputStream(content),
                Map.copyOf(metadata)
        );
        return fallback.parse(retrySource);
    }

    private String endpoint(Map<String, Object> metadata) {
        Object endpoint = metadata == null ? null : metadata.get("externalParserEndpoint");
        return endpoint == null || String.valueOf(endpoint).isBlank() ? defaultEndpoint : String.valueOf(endpoint);
    }

    private static Duration resolveTimeout(Map<String, Object> metadata) {
        if (metadata == null) {
            return DEFAULT_TIMEOUT;
        }
        Object raw = metadata.get("externalParserTimeoutSeconds");
        if (raw instanceof Number number) {
            return Duration.ofSeconds(Math.max(1, number.longValue()));
        }
        if (raw != null) {
            try {
                return Duration.ofSeconds(Math.max(1, Long.parseLong(String.valueOf(raw).trim())));
            } catch (NumberFormatException ignored) {
                return DEFAULT_TIMEOUT;
            }
        }
        return DEFAULT_TIMEOUT;
    }

    private static HttpClient defaultHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private static String parserCode(Map<String, Object> metadata) {
        Object parserCode = metadata == null ? null : metadata.get("parserCode");
        if (parserCode == null || String.valueOf(parserCode).isBlank()) {
            return "external";
        }
        return String.valueOf(parserCode);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "untitled";
    }
}
