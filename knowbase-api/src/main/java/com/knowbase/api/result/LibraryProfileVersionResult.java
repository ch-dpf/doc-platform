package com.knowbase.api.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LibraryProfileVersionResult(
        UUID profileId,
        UUID libraryId,
        int version,
        Instant createdAt,
        boolean l1Changed,
        boolean l2Changed,
        List<String> changedFields,
        List<String> suggestedActions
) {
}
