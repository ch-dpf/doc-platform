package com.knowbase.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "知识库分块配置档统计")
public record ChunkProfileSummaryResponse(
        @Schema(description = "分块档 ID", example = "cp_a1b2c3d4e5f6") String chunkProfileId,
        @Schema(description = "文档数") int docCount,
        @Schema(description = "分块数") int chunkCount,
        @Schema(description = "是否为当前主档") boolean primary) {}
