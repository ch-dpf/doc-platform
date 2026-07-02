package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "批量删除文档结果")
public record BatchDeleteDocumentsResult(
        @Schema(description = "知识库 ID")
        UUID libraryId,
        @Schema(description = "成功删除的文档数量")
        int deletedCount,
        @Schema(description = "已删除的文档 ID 列表")
        List<UUID> documentIds
) {
}
