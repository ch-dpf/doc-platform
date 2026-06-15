package com.knowbase.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "分块档历史数据回填结果")
public record ChunkProfileBackfillResponse(
        @Schema(description = "回填文档数") int backfilledDocs,
        @Schema(description = "更新分块 metadata 数") int updatedChunks) {}
