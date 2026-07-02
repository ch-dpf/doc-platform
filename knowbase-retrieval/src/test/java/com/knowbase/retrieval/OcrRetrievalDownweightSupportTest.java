package com.knowbase.retrieval;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrRetrievalDownweightSupportTest {

    @Test
    void downweightsLowConfidenceOcrCandidates() {
        UUID libraryId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        UUID indexVersionId = UUID.randomUUID();
        RetrievalCandidate candidate = new RetrievalCandidate(
                libraryId,
                documentId,
                chunkId,
                indexVersionId,
                "low confidence text",
                1.0d,
                Map.of("ocrDownweightFactor", 0.4d, "lowConfidenceOcr", true)
        );
        DefaultRetrievalPostProcessor processor = new DefaultRetrievalPostProcessor();
        RetrievalCandidate processed = processor.fuse(List.of(candidate), Map.of()).getFirst();
        assertTrue(processed.score() < 1.0d);
    }
}
