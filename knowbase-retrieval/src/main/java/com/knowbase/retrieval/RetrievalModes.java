package com.knowbase.retrieval;

import java.util.Locale;
import java.util.Map;

public final class RetrievalModes {

    public static final String POLICY_KEY = "retrievalMode";
    public static final String VECTOR = "vector";
    public static final String KEYWORD = "keyword";
    public static final String HYBRID = "hybrid";
    public static final String KEYWORD_BOOST_KEY = "keywordBoostWeight";
    public static final double DEFAULT_KEYWORD_BOOST = 0.2d;

    private RetrievalModes() {
    }

    public static String resolve(Map<String, Object> policy) {
        if (policy == null || policy.get(POLICY_KEY) == null) {
            return HYBRID;
        }
        String value = String.valueOf(policy.get(POLICY_KEY)).trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case VECTOR, "vector_only", "semantic" -> VECTOR;
            case KEYWORD, "fulltext", "full_text", "text" -> KEYWORD;
            case HYBRID, "mixed" -> HYBRID;
            default -> HYBRID;
        };
    }

    public static double keywordBoost(Map<String, Object> policy) {
        if (policy == null || policy.get(KEYWORD_BOOST_KEY) == null) {
            return DEFAULT_KEYWORD_BOOST;
        }
        Object value = policy.get(KEYWORD_BOOST_KEY);
        if (value instanceof Number number) {
            return Math.max(0.0d, number.doubleValue());
        }
        try {
            return Math.max(0.0d, Double.parseDouble(String.valueOf(value)));
        } catch (NumberFormatException exception) {
            return DEFAULT_KEYWORD_BOOST;
        }
    }
}
