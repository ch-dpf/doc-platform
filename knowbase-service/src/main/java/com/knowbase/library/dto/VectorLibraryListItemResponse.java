package com.knowbase.library.dto;

import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.domain.LibraryStatus;
import com.knowbase.library.domain.VectorLibrary;
import com.knowbase.platform.JsonSupport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 列表页轻量展示字段（对齐建库短表单：名称、描述、标签 + 运行统计）。 */
@Schema(description = "知识库列表项（轻量字段，不含完整 libraryConfig）")
public record VectorLibraryListItemResponse(
        @Schema(description = "知识库 ID") UUID libraryId,
        @Schema(description = "租户 ID") String tenantId,
        @Schema(description = "名称") String name,
        @Schema(description = "简介") String description,
        @Schema(description = "状态") LibraryStatus status,
        @Schema(description = "文档数") int documentCount,
        @Schema(description = "分块数") int chunkCount,
        @Schema(description = "待迁移到主档的已解析文档数") int pendingMigrationCount,
        @Schema(description = "标签") List<String> tags,
        @Schema(description = "Embedding 模型") String embeddingModel,
        @Schema(description = "创建时间") Instant createdAt,
        @Schema(description = "更新时间") Instant updatedAt) {

    public static VectorLibraryListItemResponse from(VectorLibrary lib) {
        return from(lib, 0);
    }

    public static VectorLibraryListItemResponse from(VectorLibrary lib, int pendingMigrationCount) {
        VectorLibraryConfig cfg = JsonSupport.parseLibraryConfig(lib.getConfigJson());
        String embeddingModel = cfg.getEmbeddingModel();
        if (embeddingModel == null || embeddingModel.isBlank()) {
            embeddingModel = "nomic-embed-text";
        }
        return new VectorLibraryListItemResponse(
                lib.getLibraryId(),
                lib.getTenantId(),
                lib.getName(),
                lib.getDescription(),
                lib.getStatus(),
                lib.getDocumentCount(),
                lib.getChunkCount(),
                Math.max(0, pendingMigrationCount),
                cfg.getTags() != null ? cfg.getTags() : List.of(),
                embeddingModel,
                lib.getCreatedAt(),
                lib.getUpdatedAt());
    }
}
