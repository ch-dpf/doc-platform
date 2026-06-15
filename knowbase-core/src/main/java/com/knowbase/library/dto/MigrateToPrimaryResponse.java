package com.knowbase.library.dto;

import java.util.UUID;

public record MigrateToPrimaryResponse(
        int candidateCount,
        String message,
        String primaryChunkProfileId,
        UUID jobId) {}
