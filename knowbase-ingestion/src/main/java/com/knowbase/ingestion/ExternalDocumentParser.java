package com.knowbase.ingestion;

import com.knowbase.ingestion.external.ExternalParserClientOptions;
import com.knowbase.ingestion.external.ExternalParserFallbackReason;
import com.knowbase.ingestion.external.ExternalParserFallbackResolver;
import com.knowbase.ingestion.external.ExternalParserHttpInvoker;
import com.knowbase.ingestion.external.ExternalParserResponseMapper;
import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional adapter for external parsers such as Docling or Unstructured.
 */
public final class ExternalDocumentParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(ExternalDocumentParser.class);

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
        ExternalParserClientOptions options = ExternalParserClientOptions.from(source.metadata());
        final byte[] content;
        try {
            content = source.inputStream().readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("读取文档失败: " + source.sourceUri(), exception);
        }
        String parserCode = parserCode(source.metadata());
        try {
            ExternalParserHttpInvoker.InvokeResult invokeResult = ExternalParserHttpInvoker.invoke(
                    httpClient,
                    endpoint,
                    content,
                    source,
                    parserCode,
                    source.metadata(),
                    options
            );
            if (invokeResult.statusCode() < 200 || invokeResult.statusCode() >= 300) {
                throw new IllegalStateException("外部解析器返回状态码: " + invokeResult.statusCode());
            }
            Map<String, Object> metadata = baseMetadata(source, endpoint, invokeResult, options);
            ParsedDocument parsed = ExternalParserResponseMapper.map(
                    source.sourceUri(),
                    firstNonBlank(source.filename(), source.sourceUri()),
                    invokeResult.body(),
                    parserCode,
                    Map.copyOf(metadata)
            );
            parsed = ParsedDocumentParseEnricher.enrich(parsed);
            log.info(
                    "外部解析完成: sourceUri={}, endpoint={}, externalParseMs={}, attempts={}, fallbackUsed=false",
                    source.sourceUri(),
                    endpoint,
                    invokeResult.durationMs(),
                    invokeResult.attempts()
            );
            return attachTiming(parsed, invokeResult, false, null);
        } catch (IOException | RuntimeException exception) {
            return fallbackParse(source, content, options, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return fallbackParse(source, content, options, exception);
        }
    }

    private ParsedDocument fallbackParse(
            DocumentSource source,
            byte[] content,
            ExternalParserClientOptions options,
            Exception cause
    ) {
        String reasonCode = ExternalParserFallbackReason.classify(cause, extractStatusCode(cause));
        if (options.failOnExternalError() || !options.fallbackEnabled()) {
            log.warn(
                    "外部解析失败且未启用 fallback: sourceUri={}, reasonCode={}, message={}",
                    source.sourceUri(),
                    reasonCode,
                    cause.getMessage()
            );
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
        metadata.put("externalParserFallbackUsed", true);
        metadata.put("externalParserFallbackReason", reasonCode);
        metadata.put("externalParserFallbackDetail", cause.getMessage() == null
                ? cause.getClass().getSimpleName()
                : cause.getMessage());
        metadata.put("externalParserFallbackParser", fallback.getClass().getSimpleName());
        log.warn(
                "外部解析回退内置 parser: sourceUri={}, reasonCode={}, fallbackParser={}",
                source.sourceUri(),
                reasonCode,
                fallback.getClass().getSimpleName()
        );
        DocumentSource retrySource = new DocumentSource(
                source.sourceUri(),
                source.filename(),
                source.mimeType(),
                new ByteArrayInputStream(content),
                Map.copyOf(metadata)
        );
        return fallback.parse(retrySource);
    }

    private static ParsedDocument attachTiming(
            ParsedDocument parsed,
            ExternalParserHttpInvoker.InvokeResult invokeResult,
            boolean fallbackUsed,
            String fallbackReason
    ) {
        Map<String, Object> metadata = new HashMap<>(parsed.metadata());
        metadata.put("externalParseMs", invokeResult.durationMs());
        metadata.put("externalParserAttempts", invokeResult.attempts());
        metadata.put("externalParserFallbackUsed", fallbackUsed);
        if (fallbackReason != null) {
            metadata.put("externalParserFallbackReason", fallbackReason);
        }
        return new ParsedDocument(
                parsed.sourceUri(),
                parsed.title(),
                parsed.text(),
                parsed.contentFamily(),
                Map.copyOf(metadata),
                parsed.blocks()
        );
    }

    private static Map<String, Object> baseMetadata(
            DocumentSource source,
            String endpoint,
            ExternalParserHttpInvoker.InvokeResult invokeResult,
            ExternalParserClientOptions options
    ) {
        Map<String, Object> metadata = new HashMap<>();
        if (source.metadata() != null) {
            metadata.putAll(source.metadata());
        }
        metadata.put("externalParserEndpoint", endpoint);
        metadata.put("externalParserStatus", invokeResult.statusCode());
        metadata.put("externalParseMs", invokeResult.durationMs());
        metadata.put("externalParserAttempts", invokeResult.attempts());
        metadata.put("externalParserFallbackUsed", false);
        metadata.put("externalParserUseJsonRequest", options.useJsonRequest());
        return metadata;
    }

    private static int extractStatusCode(Throwable cause) {
        if (cause == null || cause.getMessage() == null) {
            return 0;
        }
        String message = cause.getMessage();
        int index = message.indexOf("状态码:");
        if (index < 0) {
            index = message.indexOf("status code:");
        }
        if (index < 0) {
            return 0;
        }
        String tail = message.substring(index).replaceAll("[^0-9]", " ").trim();
        if (tail.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(tail.split("\\s+")[0]);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String endpoint(Map<String, Object> metadata) {
        Object endpoint = metadata == null ? null : metadata.get("externalParserEndpoint");
        return endpoint == null || String.valueOf(endpoint).isBlank() ? defaultEndpoint : String.valueOf(endpoint);
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
