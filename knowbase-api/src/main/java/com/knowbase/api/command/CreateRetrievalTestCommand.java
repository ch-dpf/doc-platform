package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "智能体检索测试请求")
public record CreateRetrievalTestCommand(
        @Schema(description = "测试问题", example = "如何安装 PostgreSQL 和 pgvector？")
        @NotBlank String question,
        @Schema(description = "指定智能体版本 ID，不传则使用最新已发布版本")
        UUID agentVersionId,
        @Schema(description = "调试知识库 ID 列表，不传则使用智能体绑定库并执行路由")
        List<UUID> debugLibraryIds,
        @Schema(description = "临时覆盖检索策略")
        Map<String, Object> retrievalPolicyOverride,
        @Schema(description = "临时覆盖回答策略，主要用于 maxContextTokens")
        Map<String, Object> answerPolicyOverride
) {
}
