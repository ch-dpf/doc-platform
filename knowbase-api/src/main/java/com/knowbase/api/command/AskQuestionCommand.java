package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "问答请求")
public record AskQuestionCommand(
        @Schema(description = "智能体 ID")
        @NotNull UUID agentId,
        @Schema(description = "智能体版本 ID，不指定则使用最新发布版本")
        UUID agentVersionId,
        @Schema(description = "会话 ID，用于多轮对话上下文关联")
        String sessionId,
        @Schema(description = "用户问题", example = "如何创建知识库？")
        @NotBlank String question,
        @Schema(description = "调试模式下指定检索的知识库 ID 列表")
        List<UUID> debugLibraryIds,
        @Schema(description = "模板变量")
        Map<String, Object> variables,
        @Schema(description = "是否流式响应", example = "false")
        boolean stream
) {
}
