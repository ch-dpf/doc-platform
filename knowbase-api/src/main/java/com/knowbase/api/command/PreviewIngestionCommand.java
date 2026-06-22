package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 入库预览请求，执行完整 parse → normalize → chunk 但不持久化。
 * <p>
 * 返回 {@link com.knowbase.api.result.DocumentPreviewResult#parse} 供前端展示结构块与解析器信息。
 */
@Schema(description = "入库分段预览请求")
public record PreviewIngestionCommand(
        @Schema(description = "知识库 ID")
        @NotNull UUID libraryId,
        @Schema(description = "源文件 URI 列表")
        @NotEmpty List<String> sourceUris,
        @Schema(description = "文档 Profile 编码，留空时按文件名 / MIME 自动路由")
        String documentProfileCode,
        @Schema(description = "扩展选项：parseMode、parserCode、maxPreviewBlocks、maxPreviewChunks 等")
        Map<String, Object> options
) {
}
