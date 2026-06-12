package com.knowbase.library.dto.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 分块向量化：库级仅暴露分块大小与重叠；min/max 等合并规则由系统 chunking.* 决定。
 * 数值边界与 {@link com.knowbase.vector.dto.ChunkPreviewRequest}、
 * {@link com.knowbase.pipeline.config.IngestProfileSupport} 一致。
 */
@Schema(description = "分块向量化配置（库配置 Tab：分块向量化）")
public record LibraryIndexPipelineDto(
        @Min(100)
        @Max(8000)
        @Schema(description = "目标分块字符数", example = "500", minimum = "100", maximum = "8000")
        int chunkSize,
        @Min(0)
        @Max(2000)
        @Schema(
                description = "相邻分块重叠字符数（超长段定长切时生效）",
                example = "120",
                minimum = "0",
                maximum = "2000")
        int chunkOverlap,
        @Schema(description = "Embedding 模型名称", example = "nomic-embed-text") String embeddingModel,
        @Schema(description = "向量维度", example = "768") int embeddingDimension,
        @Schema(description = "是否启用父子块（heading-level 长文档）", example = "true")
        boolean hierarchicalChunkingEnabled,
        @Size(max = 64)
        @Schema(
                description = "自定义分隔符；支持字面量 \\n。非空时优先按分隔符切段",
                example = "")
        String chunkDelimiter) {}
