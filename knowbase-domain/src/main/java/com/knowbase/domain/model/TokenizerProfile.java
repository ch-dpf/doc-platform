package com.knowbase.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TokenizerProfile(
        UUID tokenizerProfileId,
        String provider,
        String modelName,
        String tokenizerId,
        String tokenizerVersion,
        boolean approximate,
        Map<String, Object> config,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
