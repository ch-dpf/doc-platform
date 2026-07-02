package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "问答运行结果")
public record QueryRunResult(
        @Schema(description = "问答运行 ID")
        UUID queryRunId,
        @Schema(description = "智能体 ID")
        UUID agentId,
        @Schema(description = "智能体版本 ID")
        UUID agentVersionId,
        @Schema(description = "运行状态", example = "SUCCEEDED")
        String status,
        @Schema(description = "用户问题")
        String question,
        @Schema(description = "生成的回答")
        String answer,
        @Schema(description = "引用列表")
        List<CitationResult> citations,
        @Schema(description = "检索证据列表")
        List<EvidenceResult> evidence,
        @Schema(description = "Token 用量统计")
        TokenUsageResult tokenUsage,
        @Schema(description = "链路追踪 ID")
        String traceId,
        @Schema(description = "创建时间")
        Instant createdAt,
        @Schema(description = "完成时间")
        Instant completedAt
) {
}
