package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "智能体检索测试结果")
public record RetrievalTestResult(
        @Schema(description = "检索测试 ID")
        UUID retrievalTestId,
        @Schema(description = "智能体 ID")
        UUID agentId,
        @Schema(description = "智能体版本 ID")
        UUID agentVersionId,
        @Schema(description = "测试问题")
        String question,
        @Schema(description = "路由后的知识库 ID")
        List<UUID> routedLibraryIds,
        @Schema(description = "候选片段数量")
        int candidateCount,
        @Schema(description = "证据片段")
        List<EvidenceResult> evidence,
        @Schema(description = "引用列表")
        List<CitationResult> citations,
        @Schema(description = "上下文 token 数")
        int contextTokens,
        @Schema(description = "Tokenizer 标识")
        String tokenizerId,
        @Schema(description = "Tokenizer 版本")
        String tokenizerVersion,
        @Schema(description = "是否证据不足")
        boolean evidenceLow,
        @Schema(description = "调试轨迹")
        Map<String, Object> trace,
        @Schema(description = "创建时间")
        Instant createdAt
) {
}
