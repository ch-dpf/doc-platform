package com.knowbase.vector.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "批量重索引候选文档统计")
public record RebuildCandidatesResponse(
        int candidateCount,
        @Schema(description = "筛选的分块档 ID；空表示全库") String chunkProfileId) {}
