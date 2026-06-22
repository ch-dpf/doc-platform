package com.knowbase.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DefaultQuestionAnalyzer implements QuestionAnalyzer {

    @Override
    public QuestionAnalysis analyze(String question, Map<String, Object> routingPolicy) {
        String original = question == null ? "" : question.trim();
        String normalized = normalize(original);
        List<String> keywords = extractKeywords(normalized);
        List<String> expanded = expandQueries(normalized, routingPolicy);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("keywordCount", keywords.size());
        metadata.put("expandedQueryCount", expanded.size());
        return new QuestionAnalysis(original, normalized, expanded, keywords, Map.copyOf(metadata));
    }

    private static String normalize(String question) {
        if (question.isBlank()) {
            return "";
        }
        return question.replaceAll("\\s+", " ").trim();
    }

    private static List<String> extractKeywords(String normalized) {
        if (normalized.isBlank()) {
            return List.of();
        }
        String[] tokens = normalized.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHan}]+");
        Set<String> keywords = new HashSet<>();
        for (String token : tokens) {
            if (token.length() >= 2) {
                keywords.add(token);
            }
        }
        return List.copyOf(keywords);
    }

    private static List<String> expandQueries(String normalized, Map<String, Object> routingPolicy) {
        List<String> expanded = new ArrayList<>();
        if (!normalized.isBlank()) {
            expanded.add(normalized);
        }
        boolean enableExpansion = routingPolicy != null && Boolean.TRUE.equals(routingPolicy.get("expandQueries"));
        if (!enableExpansion || normalized.isBlank()) {
            return List.copyOf(expanded);
        }
        String[] parts = normalized.split("[?？;；]");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isBlank() && !trimmed.equals(normalized)) {
                expanded.add(trimmed);
            }
        }
        return List.copyOf(expanded);
    }
}
