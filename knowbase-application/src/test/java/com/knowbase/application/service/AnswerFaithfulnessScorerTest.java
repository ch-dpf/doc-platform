package com.knowbase.application.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnswerFaithfulnessScorerTest {

    @Test
    void scoresGroundedSentencesHigher() {
        String grounded = "Install PostgreSQL with pgvector extension for vector search.";
        double score = AnswerFaithfulnessScorer.score(grounded, List.of(grounded));
        assertEquals(1.0, score, 0.001);
    }

    @Test
    void returnsZeroForEmptyAnswer() {
        assertEquals(0.0, AnswerFaithfulnessScorer.score("", List.of("evidence")));
    }
}
