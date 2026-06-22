package com.knowbase.retrieval;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRetrievalPostProcessorTest {

    private final DefaultRetrievalPostProcessor postProcessor = new DefaultRetrievalPostProcessor();

    @Test
    void fuseAndRerankReduceDuplicates() {
        UUID libraryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        UUID indexVersionId = UUID.randomUUID();
        List<RetrievalCandidate> candidates = List.of(
                new RetrievalCandidate(libraryId, documentId, chunkId, indexVersionId, "same content block", 0.9d, Map.of()),
                new RetrievalCandidate(libraryId, documentId, chunkId, indexVersionId, "same content block", 0.8d, Map.of())
        );
        List<RetrievalCandidate> fused = postProcessor.fuse(candidates, Map.of(
                "fusion", "score",
                "maxCandidates", 4,
                "deduplicateByChunk", true
        ));
        assertEquals(1, fused.size());
        List<RetrievalCandidate> reranked = postProcessor.rerank(fused, Map.of("rerank", "none", "maxCandidates", 4));
        assertTrue(reranked.size() <= fused.size());
    }
}
