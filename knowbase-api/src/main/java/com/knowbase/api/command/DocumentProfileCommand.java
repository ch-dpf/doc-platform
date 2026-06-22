package com.knowbase.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

@Schema(description = "文档 Profile 配置")
public record DocumentProfileCommand(
        @Schema(description = "文档 Profile 编码，用于入库时通过 documentProfileCode 指定", example = "default_rich_text")
        String code,
        @Schema(description = "内容族（文档类型分类）", example = "RICH_TEXT")
        @NotNull String contentFamily,
        @Schema(description = "解析器编码", example = "tika")
        @NotBlank String parserCode,
        @Schema(description = "分块策略", example = "token-window")
        @NotBlank String chunkingStrategy,
        @Schema(description = "分词器 Profile ID")
        UUID tokenizerProfileId,
        @Schema(description = "元数据 Schema 定义")
        Map<String, Object> metadataSchema,
        @Schema(description = "扩展选项")
        Map<String, Object> options
) {
}
