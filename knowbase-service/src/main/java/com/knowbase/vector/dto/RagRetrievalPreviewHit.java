package com.knowbase.vector.dto;

import java.util.UUID;

public record RagRetrievalPreviewHit(
        int rank,
        UUID chunkId,
        UUID docId,
        String fileName,
        int chunkIndex,
        double score,
        String excerpt,
        boolean headerOnlyChunk,
        String chunkProfileId,
        boolean primaryProfile) {}
