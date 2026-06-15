package com.knowbase.library.dto;

import java.util.UUID;

public record ArchiveChunkProfileResponse(
        int candidateCount,
        String message,
        String chunkProfileId,
        UUID jobId) {}
