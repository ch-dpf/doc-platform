package com.knowbase.library.dto;

import com.knowbase.library.domain.LibraryStatus;
import com.knowbase.library.domain.VectorLibrary;
import com.knowbase.library.dto.config.LibraryConfigView;
import com.knowbase.library.support.LibraryConfigViewMapper;
import com.knowbase.platform.JsonSupport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "知识库详情")
public record VectorLibraryResponse(
        @Schema(description = "知识库 ID") UUID libraryId,
        @Schema(description = "租户 ID", example = "demo") String tenantId,
        @Schema(description = "名称") String name,
        @Schema(description = "简介") String description,
        @Schema(description = "状态") LibraryStatus status,
        @Schema(description = "分节库配置（非 config_json 全量；不含容量等平台字段）") LibraryConfigView libraryConfig,
        @Schema(description = "已入库文档数") int documentCount,
        @Schema(description = "向量分块数") int chunkCount,
        @Schema(description = "创建时间") Instant createdAt,
        @Schema(description = "最近更新时间") Instant updatedAt) {

    public static VectorLibraryResponse from(VectorLibrary lib) {
        return from(lib, lib.getDocumentCount(), lib.getChunkCount());
    }

    public static VectorLibraryResponse from(VectorLibrary lib, int documentCount, int chunkCount) {
        return new VectorLibraryResponse(
                lib.getLibraryId(),
                lib.getTenantId(),
                lib.getName(),
                lib.getDescription(),
                lib.getStatus(),
                LibraryConfigViewMapper.toView(JsonSupport.parseLibraryConfig(lib.getConfigJson())),
                documentCount,
                chunkCount,
                lib.getCreatedAt(),
                lib.getUpdatedAt());
    }
}
