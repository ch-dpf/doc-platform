package com.knowbase.retrieval;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetrievalModesTest {

    @Test
    void resolvesKnownModesAndDefaultsToHybrid() {
        assertEquals(RetrievalModes.HYBRID, RetrievalModes.resolve(null));
        assertEquals(RetrievalModes.VECTOR, RetrievalModes.resolve(Map.of("retrievalMode", "vector")));
        assertEquals(RetrievalModes.KEYWORD, RetrievalModes.resolve(Map.of("retrievalMode", "fulltext")));
        assertEquals(RetrievalModes.HYBRID, RetrievalModes.resolve(Map.of("retrievalMode", "mixed")));
        assertEquals(RetrievalModes.HYBRID, RetrievalModes.resolve(Map.of("retrievalMode", "unknown")));
    }

    @Test
    void composesScoresByMode() {
        assertEquals(0.8d, RetrievalScoreComposer.finalScore(RetrievalModes.VECTOR, 0.8d, 0.5d, Map.of()), 0.0001d);
        assertEquals(0.5d, RetrievalScoreComposer.finalScore(RetrievalModes.KEYWORD, 0.8d, 0.5d, Map.of()), 0.0001d);
        assertEquals(0.9d, RetrievalScoreComposer.finalScore(RetrievalModes.HYBRID, 0.8d, 0.5d, Map.of()), 0.0001d);
    }

    @Test
    void scoresKeywordOverlap() {
        double score = KeywordScorer.overlap("install PostgreSQL", "Install PostgreSQL and pgvector", Map.of());
        assertEquals(1.0d, score, 0.0001d);
    }
}
