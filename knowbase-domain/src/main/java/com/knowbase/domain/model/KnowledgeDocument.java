package com.knowbase.domain.model;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocument(
        UUID documentId,
        UUID libraryId,
        UUID indexVersionId,
        String sourceUri,
        String title,
        Instant createdAt
) {
}
