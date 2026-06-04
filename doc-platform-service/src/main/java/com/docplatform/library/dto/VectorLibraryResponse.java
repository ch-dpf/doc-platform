package com.docplatform.library.dto;

import com.docplatform.library.config.VectorLibraryConfig;
import com.docplatform.library.domain.LibraryStatus;
import com.docplatform.library.domain.VectorLibrary;
import com.docplatform.platform.JsonSupport;

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
        VectorLibraryConfig cfg = JsonSupport.parseLibraryConfig(lib.getConfigJson());
        return new VectorLibraryResponse(
                lib.getLibraryId(),
                lib.getTenantId(),
                lib.getName(),
                lib.getDescription(),
                lib.getStatus(),
                cfg,
                lib.getDocumentCount(),
                lib.getChunkCount(),
                lib.getCreatedAt(),
                lib.getUpdatedAt());
    }

}
