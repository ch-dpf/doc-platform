package com.knowbase.vector.retrieval;

import com.knowbase.library.config.RetrievalRulesSettings;
import com.knowbase.vector.config.RagProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetrievalMinScoreResolverTest {

    @Test
    void requestOverrideWins() {
        RagProperties rag = new RagProperties();
        rag.setMinScore(0.1);
        RetrievalRulesSettings retrieval = new RetrievalRulesSettings();
        retrieval.setSimilarityThreshold(0.5);

        assertEquals(0.9, RetrievalMinScoreResolver.resolve(0.9, retrieval, rag));
    }

    @Test
    void libraryThresholdUsedWhenNoRequestOverride() {
        RagProperties rag = new RagProperties();
        rag.setMinScore(0.1);
        RetrievalRulesSettings retrieval = new RetrievalRulesSettings();
        retrieval.setSimilarityThreshold(0.45);

        assertEquals(0.45, RetrievalMinScoreResolver.resolve(null, retrieval, rag));
    }

    @Test
    void globalMinScoreWhenLibraryThresholdZero() {
        RagProperties rag = new RagProperties();
        rag.setMinScore(0.2);
        RetrievalRulesSettings retrieval = new RetrievalRulesSettings();
        retrieval.setSimilarityThreshold(0.0);

        assertEquals(0.2, RetrievalMinScoreResolver.resolve(null, retrieval, rag));
    }
}
