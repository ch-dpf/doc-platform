package com.knowbase.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "删除知识库结果")
public record DeleteVectorLibraryResponse(
        @Schema(description = "已删除的知识库 ID") UUID libraryId,
        @Schema(description = "知识库名称") String name,
        @Schema(description = "一并删除的文档数") int deletedDocuments,
        @Schema(description = "一并删除的分块数") int deletedChunks,
        @Schema(description = "结果说明") String message) {}
