package com.knowbase.ingestion.parser;

import com.knowbase.ingestion.document.ParsedDocument;
import com.knowbase.ingestion.document.ParsedDocument.ContentFamily;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Parser boundary used by ingestion profiles to turn source content into a ParsedDocument tree.
 */
public interface DocumentParser {

    boolean supports(ParseRequest request);

    ParsedDocument parse(ParseRequest request);

    record ParseRequest(
            String documentId,
            String content,
            String mediaType,
            ContentFamily contentFamily,
            Map<String, String> metadata
    ) {
        public ParseRequest {
            documentId = requireText(documentId, "documentId");
            content = Objects.requireNonNull(content, "content");
            mediaType = mediaType == null ? "" : mediaType.trim().toLowerCase(Locale.ROOT);
            metadata = copyMetadata(metadata);
        }

        public static ParseRequest html(String documentId, String html) {
            return new ParseRequest(documentId, html, "text/html", ContentFamily.WEB_PAGE, Map.of());
        }

        public static ParseRequest plainText(String documentId, String text) {
            return new ParseRequest(documentId, text, "text/plain", ContentFamily.PLAIN_TEXT, Map.of());
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static Map<String, String> copyMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
}
