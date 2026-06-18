package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.UUID;

@Schema(description = "检索证据")
public record EvidenceResult(
        @Schema(description = "证据 ID")
        UUID evidenceId,
        @Schema(description = "知识库 ID")
        UUID libraryId,
        @Schema(description = "文档 ID")
        UUID documentId,
        @Schema(description = "分块 ID")
        UUID chunkId,
        @Schema(description = "索引版本 ID")
        UUID indexVersionId,
        @Schema(description = "证据内容")
        String content,
        @Schema(description = "相关性得分")
        double score,
        @Schema(description = "元数据")
        Map<String, Object> metadata
) {
}
