package com.knowbase.api.result;

import java.util.List;
import java.util.UUID;

public record DocumentDuplicateGroupResult(
        String contentHash,
        int count,
        List<UUID> documentIds,
        List<String> sourceUris
) {
}
