package com.docplatform.vector.dto;

import java.util.UUID;

public record SearchHit(
        UUID chunkId,
        UUID docId,
        String tenantId,
        int version,
        int chunkIndex,
        String content,
        double score
) {
}
