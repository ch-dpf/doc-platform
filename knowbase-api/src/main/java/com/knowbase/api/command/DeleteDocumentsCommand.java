package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "批量删除文档请求")
public record DeleteDocumentsCommand(
        @Schema(description = "待删除的文档 ID 列表")
        List<UUID> documentIds
) {
    public DeleteDocumentsCommand {
        if (documentIds == null) {
            documentIds = List.of();
        }
    }
}
