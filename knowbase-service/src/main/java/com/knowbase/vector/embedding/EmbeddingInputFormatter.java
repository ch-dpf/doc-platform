package com.knowbase.vector.embedding;

import java.util.ArrayList;
import java.util.List;

/**
 * 为支持 task 前缀的 Embedding 模型格式化输入。
 * nomic-embed-text 要求：入库用 search_document，检索 query 用 search_query。
 */
public final class EmbeddingInputFormatter {

    public static final String NOMIC_SEARCH_QUERY_PREFIX = "search_query: ";
    public static final String NOMIC_SEARCH_DOCUMENT_PREFIX = "search_document: ";

    private EmbeddingInputFormatter() {}

    public static boolean usesNomicTaskPrefixes(String model) {
        if (model == null || model.isBlank()) {
            return false;
        }
        String normalized = model.toLowerCase().replace('_', '-');
        return normalized.contains("nomic-embed");
    }

    public static String forSearchQuery(String text, String model) {
        return applyPrefix(text, NOMIC_SEARCH_QUERY_PREFIX, model);
    }

    public static String forSearchDocument(String text, String model) {
        return applyPrefix(text, NOMIC_SEARCH_DOCUMENT_PREFIX, model);
    }

    public static List<String> forSearchDocuments(List<String> texts, String model) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<String> formatted = new ArrayList<>(texts.size());
        for (String text : texts) {
            formatted.add(forSearchDocument(text, model));
        }
        return formatted;
    }

    private static String applyPrefix(String text, String prefix, String model) {
        if (text == null) {
            return "";
        }
        String trimmed = text.strip();
        if (!usesNomicTaskPrefixes(model) || trimmed.isEmpty()) {
            return trimmed;
        }
        if (trimmed.startsWith(NOMIC_SEARCH_QUERY_PREFIX) || trimmed.startsWith(NOMIC_SEARCH_DOCUMENT_PREFIX)) {
            return trimmed;
        }
        return prefix + trimmed;
    }
}
