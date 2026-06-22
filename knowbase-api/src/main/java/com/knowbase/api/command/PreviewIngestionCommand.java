package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "入库分段预览请求")
public record PreviewIngestionCommand(
        @Schema(description = "知识库 ID")
        @NotNull UUID libraryId,
        @Schema(description = "源文件 URI 列表")
        @NotEmpty List<String> sourceUris,
        @Schema(description = "文档 Profile 编码，留空时自动选择")
        String documentProfileCode,
        @Schema(description = "扩展选项，如 maxFiles、extensions、maxPreviewChunks")
        Map<String, Object> options
) {
}
