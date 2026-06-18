package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "预设信息")
public record PresetResult(
        @Schema(description = "预设编码", example = "technical_docs")
        String code,
        @Schema(description = "预设名称", example = "技术文档库")
        String name,
        @Schema(description = "预设描述")
        String description,
        @Schema(description = "预设配置")
        Map<String, Object> config,
        @Schema(description = "是否系统内置", example = "true")
        boolean builtIn,
        @Schema(description = "是否启用", example = "true")
        boolean enabled
) {
}
