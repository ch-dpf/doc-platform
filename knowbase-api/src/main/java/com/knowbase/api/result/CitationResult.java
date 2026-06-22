package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "引用信息")
public record CitationResult(
        @Schema(description = "引用 ID")
        UUID citationId,
        @Schema(description = "知识库 ID")
        UUID libraryId,
        @Schema(description = "文档 ID")
        UUID documentId,
        @Schema(description = "分块 ID")
        UUID chunkId,
        @Schema(description = "索引版本 ID")
        UUID indexVersionId,
        @Schema(description = "来源文档标题")
        String sourceTitle,
        @Schema(description = "来源文档 URI")
        String sourceUri,
        @Schema(description = "引用片段")
        String snippet,
        @Schema(description = "相关性得分")
        double score
) {
}
