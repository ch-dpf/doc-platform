package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "文档 Profile 模板目录项")
public record DocumentProfileCatalogItemResult(
        @Schema(description = "Profile 编码", example = "default_docx")
        String code,
        @Schema(description = "中文名称")
        String nameZh,
        @Schema(description = "说明")
        String descriptionZh,
        @Schema(description = "内容族")
        String contentFamily,
        @Schema(description = "默认解析器")
        String defaultParserCode,
        @Schema(description = "默认切块策略")
        String defaultChunkingStrategy,
        @Schema(description = "切块策略中文说明")
        String chunkingStrategyLabelZh,
        @Schema(description = "路由文件扩展名")
        List<String> fileExtensions,
        @Schema(description = "建仓后可修改字段")
        List<String> configurableFields,
        @Schema(description = "建仓后不可变字段")
        List<String> immutableFields
) {
}
