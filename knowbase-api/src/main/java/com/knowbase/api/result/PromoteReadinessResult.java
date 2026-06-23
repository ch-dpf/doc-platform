package com.knowbase.api.result;

import java.util.List;
import java.util.UUID;

public record PromoteReadinessResult(
        UUID libraryId,
        UUID indexGenerationId,
        boolean ready,
        boolean blocked,
        List<String> warnings,
        List<String> blockers
) {
}
