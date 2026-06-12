package com.knowbase.vector.dto;

import java.util.UUID;

public record RagCitation(
        UUID chunkId,
        UUID docId,
        int chunkIndex,
        double score,
        String excerpt,
        String fileName,
        String chunkProfileId,
        boolean primaryProfile) {
    public RagCitation(UUID chunkId, UUID docId, int chunkIndex, double score, String excerpt) {
        this(chunkId, docId, chunkIndex, score, excerpt, null, null, false);
    }

    public RagCitation(
            UUID chunkId, UUID docId, int chunkIndex, double score, String excerpt, String fileName) {
        this(chunkId, docId, chunkIndex, score, excerpt, fileName, null, false);
    }

    public RagCitation(
            UUID chunkId,
            UUID docId,
            int chunkIndex,
            double score,
            String excerpt,
            String fileName,
            String chunkProfileId) {
        this(chunkId, docId, chunkIndex, score, excerpt, fileName, chunkProfileId, false);
    }
}
