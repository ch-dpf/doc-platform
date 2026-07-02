package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "知识库信息")
public record LibraryResult(
        @Schema(description = "知识库 ID")
        UUID libraryId,
        @Schema(description = "租户 ID")
        String tenantId,
        @Schema(description = "知识库名称")
        String name,
        @Schema(description = "知识库描述")
        String description,
        @Schema(description = "状态", example = "ACTIVE")
        String status,
        @Schema(description = "知识库类型预设编码")
        String libraryTypePresetCode,
        @Schema(description = "标签列表")
        List<String> tags,
        @Schema(description = "创建时间")
        Instant createdAt,
        @Schema(description = "更新时间")
        Instant updatedAt
) {
}
