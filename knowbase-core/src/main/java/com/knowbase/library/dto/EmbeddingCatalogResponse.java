package com.knowbase.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Embedding 索引目录（当前版本固定 Ollama + pgvector）")
public record EmbeddingCatalogResponse(
        @Schema(description = "向量化提供方", example = "ollama") String provider,
        @Schema(description = "向量存储类型", example = "pgvector") String vectorStoreType,
        @Schema(description = "本地可用 Embedding 模型") List<EmbeddingModelDescriptor> models) {}
