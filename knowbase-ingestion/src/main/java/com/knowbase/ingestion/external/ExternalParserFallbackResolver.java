package com.knowbase.ingestion.external;

import com.knowbase.ingestion.DocumentParser;
import com.knowbase.ingestion.DocumentSource;
import com.knowbase.ingestion.HtmlStructureParser;
import com.knowbase.ingestion.MarkdownStructureParser;
import com.knowbase.ingestion.PdfLayoutParser;
import com.knowbase.ingestion.PdfStructureParser;
import com.knowbase.ingestion.StructuredTableDocumentParser;
import com.knowbase.ingestion.TextStructureParser;
import com.knowbase.ingestion.TikaDocumentParser;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves built-in Java parsers when external parser calls fail.
 */
public final class ExternalParserFallbackResolver {

    private ExternalParserFallbackResolver() {
    }

    public static boolean isFallbackEnabled(Map<String, Object> metadata) {
        if (metadata == null) {
            return true;
        }
        Object raw = metadata.get("externalParserFallback");
        if (raw == null) {
            raw = metadata.get("externalParserFallbackEnabled");
        }
        if (raw == null) {
            return true;
        }
        if (raw instanceof Boolean value) {
            return value;
        }
        return !"false".equalsIgnoreCase(String.valueOf(raw));
    }

    public static Optional<DocumentParser> resolve(DocumentSource source, List<DocumentParser> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        String mimeType = source.mimeType();
        String sourceUri = source.sourceUri();
        List<Class<? extends DocumentParser>> priority = fallbackPriority(mimeType, sourceUri);
        for (Class<? extends DocumentParser> type : priority) {
            Optional<DocumentParser> match = candidates.stream()
                    .filter(type::isInstance)
                    .filter(parser -> parser.supports(sourceUri, mimeType))
                    .findFirst();
            if (match.isPresent()) {
                return match;
            }
        }
        return candidates.stream()
                .filter(parser -> !(parser instanceof com.knowbase.ingestion.ExternalDocumentParser))
                .filter(parser -> parser.supports(sourceUri, mimeType))
                .findFirst();
    }

    private static List<Class<? extends DocumentParser>> fallbackPriority(String mimeType, String sourceUri) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        String uri = sourceUri == null ? "" : sourceUri.toLowerCase(Locale.ROOT);
        if (mime.contains("pdf") || uri.endsWith(".pdf")) {
            return List.of(PdfLayoutParser.class, PdfStructureParser.class, TikaDocumentParser.class);
        }
        if (mime.contains("spreadsheet") || mime.contains("excel") || uri.endsWith(".xlsx") || uri.endsWith(".csv")) {
            return List.of(StructuredTableDocumentParser.class, TikaDocumentParser.class);
        }
        if (mime.contains("html") || uri.endsWith(".html") || uri.endsWith(".htm")) {
            return List.of(HtmlStructureParser.class, TikaDocumentParser.class);
        }
        if (mime.contains("markdown") || uri.endsWith(".md")) {
            return List.of(MarkdownStructureParser.class, TextStructureParser.class);
        }
        if (mime.contains("presentation") || uri.endsWith(".pptx")) {
            return List.of(com.knowbase.ingestion.PptxStructureParser.class, TikaDocumentParser.class);
        }
        return List.of(TextStructureParser.class, TikaDocumentParser.class);
    }
}
