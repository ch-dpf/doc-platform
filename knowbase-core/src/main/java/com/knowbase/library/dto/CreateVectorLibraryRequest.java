package com.knowbase.library.dto;

import com.knowbase.library.config.RetrievalRulesSettings;
import com.knowbase.library.dto.config.LibraryIndexPipelineDto;
import com.knowbase.library.dto.config.LibraryParsingDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 创建知识库：基本信息 + 可选流水线分节（一次提交原子写入 config_json）。
 * 省略分节时使用 {@link com.knowbase.library.config.VectorLibraryConfigFactory} 产品默认。
 */
@Schema(description = "创建知识库（基本信息 + 可选 indexPipeline / parsing / retrieval）")
public record CreateVectorLibraryRequest(
        @Schema(description = "租户 ID", example = "demo", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String tenantId,
        @Schema(description = "知识库名称", example = "研发文档库", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String name,
        @Schema(description = "简介说明（选填）", example = "存放研发规范与设计文档")
        String description,
        @Schema(description = "标签", example = "[\"研发\", \"规范\"]") List<String> tags,
        @Schema(description = "分块向量化（选填；省略则用服务端默认）")
        @Valid
        LibraryIndexPipelineDto indexPipeline,
        @Schema(description = "解析配置（选填；省略则用服务端默认）")
        @Valid
        LibraryParsingDto parsing,
        @Schema(description = "检索配置（选填；省略则用服务端默认）")
        @Valid
        RetrievalRulesSettings retrieval) {}
