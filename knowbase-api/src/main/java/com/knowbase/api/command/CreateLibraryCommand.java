package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "创建知识库请求")
public record CreateLibraryCommand(
        @Schema(description = "租户 ID", example = "default")
        @NotBlank String tenantId,
        @Schema(description = "知识库名称", example = "产品文档库")
        @NotBlank String name,
        @Schema(description = "知识库描述")
        String description,
        @Schema(description = "知识库类型预设编码", example = "general")
        @NotBlank String libraryTypePresetCode,
        @Schema(description = "标签列表")
        List<String> tags,
        @Schema(description = "知识库 Profile 配置")
        @Valid LibraryProfileCommand profile,
        @Schema(description = "文档 Profile 配置列表")
        List<@Valid DocumentProfileCommand> documentProfiles
) {
}
