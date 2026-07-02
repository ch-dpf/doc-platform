package com.knowbase.api.result;

import java.util.List;
import java.util.UUID;

public record IndexHealthResult(
        UUID libraryId,
        UUID activeGenerationId,
        UUID activeProfileId,
        UUID latestProfileId,
        boolean l1DriftDetected,
        boolean rebuildRecommended,
        List<String> driftFields,
        String message
) {
}
