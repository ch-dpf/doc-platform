package com.knowbase.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** 创建知识库：仅基本信息，流水线由服务端 defaults 写入 config_json。 */
@Schema(description = "创建知识库请求（仅基本信息；流水线使用服务端默认配置）")
public record CreateVectorLibraryRequest(
        @Schema(description = "租户 ID", example = "demo", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String tenantId,
        @Schema(description = "知识库名称", example = "研发文档库", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String name,
        @Schema(description = "简介说明", example = "存放研发规范与设计文档", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String description,
        @Schema(description = "标签", example = "[\"研发\", \"规范\"]") List<String> tags) {}
