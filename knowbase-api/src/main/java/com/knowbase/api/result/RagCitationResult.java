package com.knowbase.api.result;

import java.util.UUID;

public record RagCitationResult(
        UUID chunkId,
        UUID docId,
        int chunkIndex,
        double score,
        String excerpt,
        String fileName) {}
