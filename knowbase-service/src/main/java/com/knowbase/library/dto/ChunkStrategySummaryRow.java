package com.knowbase.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "按文件类型的生效分块策略摘要（只读）")
public record ChunkStrategySummaryRow(
        @Schema(description = "文件类型标识", example = "pdf") String fileType,
        @Schema(description = "文件类型展示名", example = "PDF") String fileTypeLabel,
        @Schema(description = "分块策略 wire 值", example = "paragraph-first") String chunkingStrategy,
        @Schema(description = "分块策略中文名", example = "按段落") String chunkingStrategyLabel,
        @Schema(description = "长文档是否可能启用父子块") boolean hierarchicalWhenApplicable,
        @Schema(description = "解析要点", example = "OCR 随系统开关") String parsingNote) {}
