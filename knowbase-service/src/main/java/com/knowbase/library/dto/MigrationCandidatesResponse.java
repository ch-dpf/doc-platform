package com.knowbase.library.dto;

import java.util.List;

public record MigrationCandidatesResponse(
        String primaryChunkProfileId,
        int candidateCount,
        List<MigrationProfileBreakdown> profileBreakdown) {}
