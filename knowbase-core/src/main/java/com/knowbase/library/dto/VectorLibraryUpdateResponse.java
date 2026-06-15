package com.knowbase.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "知识库分节配置更新结果")
public record VectorLibraryUpdateResponse(
        @Schema(description = "更新后的知识库详情（含递增后的 configVersion）") VectorLibraryResponse library,
        @Schema(
                description = "非阻断性警告（如 Embedding 变更建议重索引）",
                example = "[\"Embedding 模型、维度或提供方已变更：已有向量与检索可能不一致，请在文档库中对相关文档执行补偿重索引。\"]")
        List<String> warnings) {}
