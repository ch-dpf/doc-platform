package com.knowbase.api.result;

import java.util.UUID;

public record IndexGenerationRebuildResult(
        IndexVersionResult generation,
        IngestionRunResult ingestionRun,
        UUID previousActiveGenerationId,
        boolean promoted
) {
}
