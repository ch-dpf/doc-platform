package com.knowbase.domain.model;

import com.knowbase.domain.status.IndexVersionStatus;

import java.time.Instant;
import java.util.UUID;

public record IndexVersion(
        UUID indexVersionId,
        UUID libraryId,
        UUID profileId,
        int version,
        IndexVersionStatus status,
        int documentCount,
        int chunkCount,
        Instant publishedAt,
        Instant createdAt
) {
}
