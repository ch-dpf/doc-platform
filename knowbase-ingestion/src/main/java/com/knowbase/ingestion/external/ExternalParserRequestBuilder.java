package com.knowbase.ingestion.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.knowbase.ingestion.DocumentSource;

import java.util.Base64;
import java.util.Map;

/**
 * Builds optional JSON request bodies per {@code external-parser-request.schema.json}.
 */
public final class ExternalParserRequestBuilder {

    public static final String REQUEST_SCHEMA_VERSION = "1.0";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ExternalParserRequestBuilder() {
    }

    public static byte[] buildJsonBody(
            DocumentSource source,
            byte[] content,
            String parserCode,
            Map<String, Object> metadata
    ) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("schemaVersion", REQUEST_SCHEMA_VERSION);
        root.put("sourceUri", source.sourceUri() == null ? "" : source.sourceUri());
        root.put("filename", source.filename() == null ? "" : source.filename());
        root.put("mimeType", source.mimeType() == null ? "application/octet-stream" : source.mimeType());
        root.put("parserCode", parserCode == null ? "external" : parserCode);
        root.put("contentBase64", Base64.getEncoder().encodeToString(content));
        if (metadata != null && metadata.get("documentProfileCode") != null) {
            root.put("documentProfileCode", String.valueOf(metadata.get("documentProfileCode")));
        }
        if (metadata != null && metadata.get("libraryId") != null) {
            root.put("libraryId", String.valueOf(metadata.get("libraryId")));
        }
        ObjectNode options = root.putObject("options");
        if (metadata != null) {
            metadata.forEach((key, value) -> {
                if (value == null || key.startsWith("externalParser")) {
                    return;
                }
                if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                    options.putPOJO(key, value);
                }
            });
        }
        try {
            return MAPPER.writeValueAsBytes(root);
        } catch (Exception exception) {
            throw new IllegalStateException("构建外部解析器 JSON 请求失败", exception);
        }
    }
}
