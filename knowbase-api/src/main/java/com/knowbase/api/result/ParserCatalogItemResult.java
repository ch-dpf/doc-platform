package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "解析器目录项")
public record ParserCatalogItemResult(
        @Schema(description = "解析器编码", example = "docx-structure")
        String code,
        @Schema(description = "中文名称")
        String nameZh,
        @Schema(description = "能力说明")
        String descriptionZh,
        @Schema(description = "是否内置实现")
        boolean builtIn,
        @Schema(description = "是否为外接解析器")
        boolean external,
        @Schema(description = "外接时是否需要配置 endpoint")
        boolean endpointRequired,
        @Schema(description = "典型文件扩展名")
        List<String> supportedExtensions,
        @Schema(description = "能力标签")
        List<String> capabilities,
        @Schema(description = "解析器依赖健康状态")
        ParserHealthResult health
) {
}
