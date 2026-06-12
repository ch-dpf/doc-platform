package com.knowbase.library.dto;

import com.knowbase.library.dto.config.LibraryIndexPipelineDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "更新索引管道配置（库内已有向量分块时接口返回 409）")
public record UpdateLibraryIndexPipelineRequest(
        @Schema(description = "索引管道配置体", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Valid
        LibraryIndexPipelineDto indexPipeline) {}
