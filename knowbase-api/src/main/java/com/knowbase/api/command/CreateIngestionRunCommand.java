package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "创建入库任务请求")
public record CreateIngestionRunCommand(
        @Schema(description = "知识库 ID")
        @NotNull UUID libraryId,
        @Schema(description = "源文件 URI 列表", example = "[\"file:///data/docs/guide.pdf\"]")
        @NotEmpty List<String> sourceUris,
        @Schema(description = "源类型", example = "file")
        String sourceType,
        @Schema(description = "文档 Profile 编码")
        String documentProfileCode,
        @Schema(description = "扩展选项")
        Map<String, Object> options
) {
}
