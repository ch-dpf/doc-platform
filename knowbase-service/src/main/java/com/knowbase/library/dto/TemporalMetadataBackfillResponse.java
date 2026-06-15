package com.knowbase.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "分块时间元数据历史回填结果")
public record TemporalMetadataBackfillResponse(
        @Schema(description = "处理文档数") int processedDocs,
        @Schema(description = "更新分块 metadata 数") int updatedChunks,
        @Schema(description = "跳过（已有时间字段）分块数") int skippedChunks) {}
