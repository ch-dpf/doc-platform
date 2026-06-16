package com.knowbase.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "内置解析器描述")
public record ParserEngineDescriptor(
        @Schema(description = "解析器 ID", example = "tika-structured") String parserId,
        @Schema(description = "展示名称", example = "结构化文档") String label,
        @Schema(description = "说明") String description,
        @Schema(description = "推荐适用的文件类型") List<String> recommendedFileTypes) {}
