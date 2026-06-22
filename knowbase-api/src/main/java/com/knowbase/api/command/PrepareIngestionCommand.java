package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 入库准备阶段请求，支持分阶段调试解析 / 清洗 / 切块流水线。
 * <p>
 * 解析相关 options 键：
 * <ul>
 *   <li>{@code parseMode} — standard | layout | ocr，决定 parser 路由策略</li>
 *   <li>{@code parserCode} — 显式指定解析器，如 docx-structure、qa、pdf-layout</li>
 *   <li>{@code maxPreviewBlocks} / {@code maxPreviewChars} — 解析结果预览裁剪上限</li>
 * </ul>
 */
@Schema(description = "入库准备阶段请求（解析 / 清洗 / 分段）")
public record PrepareIngestionCommand(
        @Schema(description = "知识库 ID")
        @NotNull UUID libraryId,
        @Schema(description = "源文件 URI 列表")
        @NotEmpty List<String> sourceUris,
        @Schema(description = "文档 Profile 编码，留空时自动选择；Profile 含默认 parserCode")
        String documentProfileCode,
        @Schema(description = "准备阶段：parse（仅解析）| normalize（解析+清洗）| chunk（全流程）| all")
        String prepareStage,
        @Schema(description = "扩展选项，含 parseMode、parserCode、maxPreviewBlocks 等")
        Map<String, Object> options
) {
}
