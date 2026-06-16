package com.knowbase.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "本地 Ollama 已拉取且支持 Embedding 的模型")
public record EmbeddingModelDescriptor(
        @Schema(description = "模型 ID（不含 :tag 后缀）", example = "nomic-embed-text") String modelId,
        @Schema(description = "向量维度", example = "768") int dimension) {}
