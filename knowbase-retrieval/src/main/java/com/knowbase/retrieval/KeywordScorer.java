package com.knowbase.retrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KeywordScorer {

    private KeywordScorer() {
    }

    public static double overlap(String question, String content, Map<String, Object> policy) {
        List<String> tokens = queryTokens(question, policy);
        if (tokens.isEmpty() || content == null) {
            return 0.0d;
        }
        String lowerContent = content.toLowerCase(Locale.ROOT);
        int hits = 0;
        for (String token : tokens) {
            if (lowerContent.contains(token)) {
                hits++;
            }
        }
        return (double) hits / tokens.size();
    }

    public static List<String> queryTokens(String question, Map<String, Object> policy) {
        List<String> tokens = new ArrayList<>();
        if (question != null && !question.isBlank()) {
            for (String token : question.toLowerCase(Locale.ROOT).split("\\s+")) {
                if (token.length() > 1) {
                    tokens.add(token);
                }
            }
        }
        Object keywords = policy == null ? null : policy.get("queryKeywords");
        if (keywords instanceof List<?> keywordList) {
            for (Object keyword : keywordList) {
                String value = String.valueOf(keyword).toLowerCase(Locale.ROOT);
                if (value.length() > 1) {
                    tokens.add(value);
                }
            }
        }
        return tokens;
    }
}
