package com.knowbase.library.dto;

import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.domain.LibraryStatus;
import com.knowbase.library.domain.VectorLibrary;
import com.knowbase.platform.JsonSupport;

import java.time.Instant;
import java.util.UUID;

public record VectorLibraryResponse(
        UUID libraryId,
        String tenantId,
        String name,
        String description,
        LibraryStatus status,
        VectorLibraryConfig config,
        int documentCount,
        int chunkCount,
        Instant createdAt,
        Instant updatedAt) {

    public static VectorLibraryResponse from(VectorLibrary lib) {
        return from(lib, lib.getDocumentCount(), lib.getChunkCount());
    }

    public static VectorLibraryResponse from(VectorLibrary lib, int documentCount, int chunkCount) {
        VectorLibraryConfig cfg = JsonSupport.parseLibraryConfig(lib.getConfigJson());
        return new VectorLibraryResponse(
                lib.getLibraryId(),
                lib.getTenantId(),
                lib.getName(),
                lib.getDescription(),
                lib.getStatus(),
                cfg,
                documentCount,
                chunkCount,
                lib.getCreatedAt(),
                lib.getUpdatedAt());
    }

}
