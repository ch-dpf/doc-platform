package com.knowbase.api.result;

import java.time.Instant;
import java.util.UUID;

public record IndexVersionResult(
        UUID indexVersionId,
        UUID libraryId,
        UUID profileId,
        int version,
        String status,
        int documentCount,
        int chunkCount,
        Instant publishedAt,
        Instant createdAt
) {
}
