package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "入库任务信息")
public record IngestionRunResult(
        @Schema(description = "入库任务 ID")
        UUID runId,
        @Schema(description = "知识库 ID")
        UUID libraryId,
        @Schema(description = "任务状态", example = "SUCCEEDED")
        String status,
        @Schema(description = "输入文档总数")
        int inputDocuments,
        @Schema(description = "成功处理文档数")
        int succeededDocuments,
        @Schema(description = "失败文档数")
        int failedDocuments,
        @Schema(description = "生成的分块总数")
        int chunkCount,
        @Schema(description = "索引版本 ID")
        UUID indexVersionId,
        @Schema(description = "Pipeline Trace ID，可用于观测页查询 Span")
        UUID traceId,
        @Schema(description = "状态消息或错误信息")
        String message,
        @Schema(description = "创建时间")
        Instant createdAt,
        @Schema(description = "更新时间")
        Instant updatedAt
) {
}
