package com.knowbase.vector.dto;

import java.util.UUID;

public record RebuildLibraryResponse(
        int candidateCount,
        String message,
        String chunkProfileId,
        UUID jobId) {
    public RebuildLibraryResponse(int candidateCount, String message) {
        this(candidateCount, message, null, null);
    }

    public RebuildLibraryResponse(int candidateCount, String message, String chunkProfileId) {
        this(candidateCount, message, chunkProfileId, null);
    }
}
