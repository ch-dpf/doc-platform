package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "入库准备阶段请求（解析 / 清洗 / 分段）")
public record PrepareIngestionCommand(
        @Schema(description = "知识库 ID")
        @NotNull UUID libraryId,
        @Schema(description = "源文件 URI 列表")
        @NotEmpty List<String> sourceUris,
        @Schema(description = "文档 Profile 编码，留空时自动选择")
        String documentProfileCode,
        @Schema(description = "准备阶段：parse | normalize | chunk | all")
        String prepareStage,
        @Schema(description = "扩展选项")
        Map<String, Object> options
) {
}
