package com.knowbase.domain.model;

import com.knowbase.domain.status.LibraryStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record KnowledgeLibrary(
        UUID libraryId,
        String tenantId,
        String name,
        String description,
        LibraryStatus status,
        String libraryTypePresetCode,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
}
