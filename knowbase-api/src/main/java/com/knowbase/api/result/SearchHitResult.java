package com.knowbase.api.result;

import java.util.UUID;

public record SearchHitResult(
        UUID chunkId,
        UUID docId,
        int chunkIndex,
        double score,
        String content,
        String chunkProfileId) {}
