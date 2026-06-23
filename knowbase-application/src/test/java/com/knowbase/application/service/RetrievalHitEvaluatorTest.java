package com.knowbase.application.service;

import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.model.RetrievalEvalSample;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.status.DocumentStatus;
import com.knowbase.retrieval.RetrievalCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetrievalHitEvaluatorTest {

    private static final UUID DOCUMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CHUNK_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private InMemoryKnowbaseRepository repository;
    private RetrievalHitEvaluator evaluator;

    @BeforeEach
    void setUp() {
        repository = new InMemoryKnowbaseRepository();
        evaluator = new RetrievalHitEvaluator(repository);
        repository.saveDocument(new KnowledgeDocument(
                DOCUMENT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "docs/guide.pdf",
                "Guide",
                DocumentStatus.INDEXED,
                null,
                null,
                Instant.now(),
                null,
                Instant.now(),
                Instant.now()
        ));
    }

    @Test
    void hitsByDocumentIdWithinTopK() {
        RetrievalEvalSample sample = sample(List.of(DOCUMENT_ID), List.of(), List.of(), 3);
        var evaluation = evaluator.evaluate(sample, List.of(
                candidate(UUID.randomUUID(), UUID.randomUUID(), "other"),
                candidate(DOCUMENT_ID, CHUNK_ID, "matched by document id")
        ));
        assertTrue(evaluation.hit());
        assertEquals(2, evaluation.firstHitRank());
        assertEquals("DOCUMENT_ID", evaluation.matchType());
    }

    @Test
    void hitsBySourceUriFilename() {
        RetrievalEvalSample sample = sample(List.of(), List.of("guide.pdf"), List.of(), 5);
        var evaluation = evaluator.evaluate(sample, List.of(
                candidate(UUID.randomUUID(), UUID.randomUUID(), "miss"),
                candidate(DOCUMENT_ID, CHUNK_ID, "matched by source uri")
        ));
        assertTrue(evaluation.hit());
        assertEquals("SOURCE_URI", evaluation.matchType());
    }

    @Test
    void hitsByGroundTruthContextSubstring() {
        RetrievalEvalSample sample = sample(
                List.of(),
                List.of(),
                List.of("install postgresql with pgvector extension"),
                8
        );
        var evaluation = evaluator.evaluate(sample, List.of(
                candidate(UUID.randomUUID(), UUID.randomUUID(), "unrelated"),
                candidate(DOCUMENT_ID, CHUNK_ID, "Steps to install PostgreSQL with pgvector extension on Linux")
        ));
        assertTrue(evaluation.hit());
        assertEquals("GROUND_TRUTH_CONTEXT", evaluation.matchType());
    }

    @Test
    void missesWhenExpectedChunkOutsideTopK() {
        RetrievalEvalSample sample = sample(List.of(DOCUMENT_ID), List.of(), List.of(), 1);
        var evaluation = evaluator.evaluate(sample, List.of(
                candidate(UUID.randomUUID(), UUID.randomUUID(), "first"),
                candidate(DOCUMENT_ID, CHUNK_ID, "second")
        ));
        assertFalse(evaluation.hit());
    }

    @Test
    void computesContextPrecisionAtK() {
        RetrievalEvalSample sample = sample(List.of(DOCUMENT_ID), List.of(), List.of(), 3);
        double precision = evaluator.contextPrecisionAtK(sample, List.of(
                candidate(DOCUMENT_ID, CHUNK_ID, "relevant one"),
                candidate(UUID.randomUUID(), UUID.randomUUID(), "noise"),
                candidate(DOCUMENT_ID, UUID.randomUUID(), "relevant two")
        ));
        assertEquals(2.0 / 3.0, precision, 0.001);
    }

    private static RetrievalEvalSample sample(
            List<UUID> documentIds,
            List<String> sourceUris,
            List<String> groundTruths,
            int hitRank
    ) {
        return new RetrievalEvalSample(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "question",
                documentIds,
                sourceUris,
                groundTruths,
                hitRank,
                null,
                true,
                Instant.now(),
                Instant.now()
        );
    }

    private static RetrievalCandidate candidate(UUID documentId, UUID chunkId, String content) {
        return new RetrievalCandidate(
                UUID.randomUUID(),
                documentId,
                chunkId,
                UUID.randomUUID(),
                content,
                0.9,
                java.util.Map.of()
        );
    }
}
