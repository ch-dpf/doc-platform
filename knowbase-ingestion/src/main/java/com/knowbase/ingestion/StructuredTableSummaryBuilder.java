package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule-based document summary for structured tables (fallback before LLM document summary).
 */
public final class StructuredTableSummaryBuilder {

    private StructuredTableSummaryBuilder() {
    }

    public static String buildDocumentSummaryText(ParsedDocument document, Map<String, List<DocumentChunk>> rowsBySheet) {
        StringBuilder content = new StringBuilder();
        content.append("Document summary: ")
                .append(firstNonBlank(document == null ? null : document.title(), "Structured table"))
                .append('\n');
        content.append("Sheets indexed: ").append(rowsBySheet.size()).append('\n');
        for (Map.Entry<String, List<DocumentChunk>> entry : rowsBySheet.entrySet()) {
            List<DocumentChunk> rows = entry.getValue();
            long indexableCount = rows.stream().filter(StructuredTableSummaryBuilder::isIndexableChunk).count();
            content.append("- ")
                    .append(safeLabel(entry.getKey()))
                    .append(": ")
                    .append(rows.size())
                    .append(" rows");
            if (indexableCount != rows.size()) {
                content.append(" (").append(indexableCount).append(" indexable)");
            }
            List<String> headers = rows.isEmpty() ? List.of() : extractHeaders(rows.getFirst());
            if (!headers.isEmpty()) {
                content.append(" (")
                        .append(headers.size())
                        .append(" cols: ")
                        .append(String.join(", ", headers.size() > 6 ? headers.subList(0, 6) : headers));
                if (headers.size() > 6) {
                    content.append(", ...");
                }
                content.append(')');
            }
            content.append('\n');
            appendRowExcerpts(content, rows);
        }
        return content.toString().trim();
    }

    private static void appendRowExcerpts(StringBuilder content, List<DocumentChunk> rows) {
        List<DocumentChunk> excerpts = rows.stream().filter(StructuredTableSummaryBuilder::isIndexableChunk).toList();
        if (excerpts.isEmpty()) {
            excerpts = rows;
        }
        int limit = Math.min(excerpts.size(), 12);
        for (int index = 0; index < limit; index++) {
            String rowText = stripPrefixes(excerpts.get(index).content());
            if (rowText.isBlank()) {
                continue;
            }
            content.append("  • ").append(truncate(rowText, 240)).append('\n');
        }
        if (rows.size() > limit) {
            content.append("  • ... ").append(excerpts.size() - limit).append(" more rows\n");
        }
    }

    static boolean isIndexableChunk(DocumentChunk chunk) {
        if (chunk == null || chunk.metadata() == null) {
            return true;
        }
        Object indexable = chunk.metadata().get("indexable");
        if (indexable instanceof Boolean value) {
            return value;
        }
        return true;
    }

    private static String truncate(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)).trim() + "...";
    }

    static Map<String, String> parseRowFields(String content) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (content == null || content.isBlank()) {
            return fields;
        }
        for (String part : content.split("\\|")) {
            String token = part.trim();
            int separator = token.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = token.substring(0, separator).trim();
            String value = token.substring(separator + 1).trim();
            if (!key.isBlank() && !value.isBlank()) {
                fields.putIfAbsent(key, value);
            }
        }
        return fields;
    }

    @SuppressWarnings("unchecked")
    static List<String> extractHeaders(DocumentChunk chunk) {
        if (chunk.metadata() == null) {
            return List.of();
        }
        Object headerPath = chunk.metadata().get("headerPath");
        if (headerPath instanceof List<?> list) {
            List<String> headers = new ArrayList<>();
            for (Object item : list) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    headers.add(String.valueOf(item).trim());
                }
            }
            if (!headers.isEmpty()) {
                return headers;
            }
        }
        return new ArrayList<>(parseRowFields(stripPrefixes(chunk.content())).keySet());
    }

    public static String stripPrefixes(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.trim();
        while (true) {
            int newline = normalized.indexOf('\n');
            if (newline > 0 && normalized.startsWith("[") && normalized.contains("]\n")) {
                normalized = normalized.substring(newline + 1).trim();
                continue;
            }
            break;
        }
        return normalized;
    }

    private static String safeLabel(String label) {
        return label == null || label.isBlank() ? "Table" : label.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
