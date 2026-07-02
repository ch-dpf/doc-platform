package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;
import java.util.UUID;

@Schema(description = "知识库 Profile 配置")
public record LibraryProfileCommand(
        @Schema(description = "向量嵌入模型提供商", example = "ollama")
        @NotBlank String embeddingProvider,
        @Schema(description = "向量嵌入模型名称", example = "bge-m3")
        @NotBlank String embeddingModel,
        @Schema(description = "向量维度", example = "1024")
        @Min(1) int embeddingDimension,
        @Schema(description = "嵌入分词器 Profile ID")
        UUID embeddingTokenizerProfileId,
        @Schema(description = "分块最大 Token 数", example = "512")
        @Min(1) int chunkMaxTokens,
        @Schema(description = "分块重叠 Token 数", example = "64")
        @Min(0) int chunkOverlapTokens,
        @Schema(description = "检索 TopK 数量", example = "5")
        @Min(1) int retrievalTopK,
        @Schema(description = "扩展选项")
        Map<String, Object> options
) {
}
