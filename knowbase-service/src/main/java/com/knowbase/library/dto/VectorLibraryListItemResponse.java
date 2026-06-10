package com.knowbase.library.dto;

import com.knowbase.library.config.IngestAccessSettings;
import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.domain.LibraryStatus;
import com.knowbase.library.domain.VectorLibrary;
import com.knowbase.platform.JsonSupport;
import com.knowbase.vector.chunk.ChunkingStrategy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 列表页轻量展示字段，不含完整 config_json。 */
public record VectorLibraryListItemResponse(
        UUID libraryId,
        String tenantId,
        String name,
        String description,
        LibraryStatus status,
        int documentCount,
        int chunkCount,
        int configVersion,
        String wizardMode,
        String accessMode,
        String chunkingStrategy,
        String embeddingModel,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt) {

    public static VectorLibraryListItemResponse from(VectorLibrary lib) {
        VectorLibraryConfig cfg = JsonSupport.parseLibraryConfig(lib.getConfigJson());
        String accessMode = IngestAccessSettings.FIXED_ACCESS_MODE;
        ChunkingStrategy strategy = cfg.getChunkingStrategy();
        return new VectorLibraryListItemResponse(
                lib.getLibraryId(),
                lib.getTenantId(),
                lib.getName(),
                lib.getDescription(),
                lib.getStatus(),
                lib.getDocumentCount(),
                lib.getChunkCount(),
                cfg.getConfigVersion(),
                cfg.getWizardMode(),
                accessMode,
                strategy != null ? strategy.toWire() : null,
                cfg.getEmbeddingModel(),
                cfg.getTags() != null ? cfg.getTags() : List.of(),
                lib.getCreatedAt(),
                lib.getUpdatedAt());
    }
}
