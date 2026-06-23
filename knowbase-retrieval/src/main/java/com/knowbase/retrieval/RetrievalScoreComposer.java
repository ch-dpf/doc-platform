package com.knowbase.retrieval;

import java.util.Map;

public final class RetrievalScoreComposer {

    private RetrievalScoreComposer() {
    }

    public static double finalScore(String retrievalMode, double vectorScore, double keywordScore, Map<String, Object> policy) {
        return switch (retrievalMode) {
            case RetrievalModes.VECTOR -> vectorScore;
            case RetrievalModes.KEYWORD -> keywordScore;
            default -> vectorScore + keywordScore * RetrievalModes.keywordBoost(policy);
        };
    }
}
