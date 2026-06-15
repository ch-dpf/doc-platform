package com.knowbase.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** 基本信息：名称、描述、标签。 */
@Schema(description = "更新基本信息（库配置 Tab：基本信息）")
public record UpdateLibraryBasicRequest(
        @Schema(description = "知识库名称", example = "研发文档库", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String name,
        @Schema(description = "简介说明", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String description,
        @Schema(description = "标签列表", example = "[\"研发\"]") List<String> tags) {}
