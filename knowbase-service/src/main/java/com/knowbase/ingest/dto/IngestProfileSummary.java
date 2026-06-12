package com.knowbase.ingest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 文档入库时采集级分块数值覆盖（只读摘要，不含 strategy / 解析 / 清洗）。 */
@Schema(description = "采集级分块数值覆盖摘要")
public record IngestProfileSummary(
        @Schema(description = "覆盖块大小（字符）") Integer chunkSize,
        @Schema(description = "覆盖重叠（字符）") Integer chunkOverlap) {}
