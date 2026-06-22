package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional adapter for external parsers such as Docling or Unstructured.
 */
public final class ExternalDocumentParser implements DocumentParser {

    private static final Pattern JSON_TEXT = Pattern.compile("\"(?:text|markdown|content)\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");

    private final HttpClient httpClient;
    private final String defaultEndpoint;

    public ExternalDocumentParser() {
        this(HttpClient.newHttpClient(), System.getenv("KNOWBASE_EXTERNAL_PARSER_ENDPOINT"));
    }

    ExternalDocumentParser(HttpClient httpClient, String defaultEndpoint) {
        this.httpClient = httpClient;
        this.defaultEndpoint = defaultEndpoint;
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
        try {
            byte[] content = source.inputStream().readAllBytes();
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Content-Type", source.mimeType() == null || source.mimeType().isBlank()
                            ? "application/octet-stream"
                            : source.mimeType())
                    .header("X-KnowBase-Source-Uri", source.sourceUri() == null ? "" : source.sourceUri())
                    .header("X-KnowBase-Parser", parserCode(source.metadata()))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("外部解析器返回状态码: " + response.statusCode());
            }
            String text = extractText(response.body());
            Map<String, Object> metadata = new HashMap<>(source.metadata());
            metadata.put("parser", parserCode(source.metadata()));
            metadata.put("externalParserEndpoint", endpoint);
            metadata.put("externalParserStatus", response.statusCode());
            return new ParsedDocument(
                    source.sourceUri(),
                    firstNonBlank(source.filename(), source.sourceUri()),
                    text,
                    ContentFamily.RICH_TEXT,
                    Map.copyOf(metadata),
                    StructureParsingSupport.parseMarkdown(text)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("外部解析器调用失败: " + source.sourceUri(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("外部解析器调用被中断: " + source.sourceUri(), exception);
        }
    }

    static String extractText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        Matcher matcher = JSON_TEXT.matcher(responseBody);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return responseBody;
    }

    private String endpoint(Map<String, Object> metadata) {
        Object endpoint = metadata == null ? null : metadata.get("externalParserEndpoint");
        return endpoint == null || String.valueOf(endpoint).isBlank() ? defaultEndpoint : String.valueOf(endpoint);
    }

    private static String parserCode(Map<String, Object> metadata) {
        Object parserCode = metadata == null ? null : metadata.get("parserCode");
        if (parserCode == null || String.valueOf(parserCode).isBlank()) {
            return "external";
        }
        return String.valueOf(parserCode);
    }

    private static String unescapeJson(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
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
