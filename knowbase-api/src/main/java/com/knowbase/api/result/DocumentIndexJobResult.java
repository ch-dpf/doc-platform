package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "单文档索引作业")
public record DocumentIndexJobResult(
        @Schema(description = "作业 ID") UUID jobId,
        @Schema(description = "入库运行 ID") UUID runId,
        @Schema(description = "知识库 ID") UUID libraryId,
        @Schema(description = "文档 ID") UUID documentId,
        @Schema(description = "源 URI") String sourceUri,
        @Schema(description = "作业状态") String status,
        @Schema(description = "当前阶段") String stage,
        @Schema(description = "成功写入的分块数") int chunkCount,
        @Schema(description = "说明") String message,
        @Schema(description = "错误信息") String errorMessage,
        @Schema(description = "创建时间") Instant createdAt,
        @Schema(description = "更新时间") Instant updatedAt
) {
}
