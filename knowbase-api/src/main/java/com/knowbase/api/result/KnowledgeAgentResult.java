package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "知识智能体信息")
public record KnowledgeAgentResult(
        @Schema(description = "智能体 ID")
        UUID agentId,
        @Schema(description = "智能体版本 ID")
        UUID agentVersionId,
        @Schema(description = "租户 ID")
        String tenantId,
        @Schema(description = "智能体名称")
        String name,
        @Schema(description = "智能体描述")
        String description,
        @Schema(description = "状态", example = "ACTIVE")
        String status,
        @Schema(description = "版本号")
        int version,
        @Schema(description = "场景预设编码")
        String scenePresetCode,
        @Schema(description = "关联知识库 ID 列表")
        List<UUID> libraryIds,
        @Schema(description = "Chat 阶段 Tokenizer Profile ID")
        UUID chatTokenizerProfileId,
        @Schema(description = "是否已发布")
        boolean published,
        @Schema(description = "创建时间")
        Instant createdAt,
        @Schema(description = "更新时间")
        Instant updatedAt
) {
}
