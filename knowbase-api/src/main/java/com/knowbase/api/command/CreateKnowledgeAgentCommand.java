package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "创建知识智能体请求")
public record CreateKnowledgeAgentCommand(
        @Schema(description = "租户 ID", example = "default")
        @NotBlank String tenantId,
        @Schema(description = "智能体名称", example = "产品问答助手")
        @NotBlank String name,
        @Schema(description = "智能体描述")
        String description,
        @Schema(description = "场景预设编码", example = "qa")
        @NotBlank String scenePresetCode,
        @Schema(description = "关联知识库 ID 列表")
        @NotEmpty List<UUID> libraryIds,
        @Schema(description = "路由策略配置")
        Map<String, Object> routingPolicy,
        @Schema(description = "检索策略配置")
        Map<String, Object> retrievalPolicy,
        @Schema(description = "回答策略配置")
        Map<String, Object> answerPolicy,
        @Schema(description = "Chat 阶段 Tokenizer Profile ID")
        UUID chatTokenizerProfileId,
        @Schema(description = "系统提示词")
        String systemPrompt
) {
}
