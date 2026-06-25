package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "建仓入库产品目录（解析器与文档 Profile 模板）")
public record IngestionCatalogResult(
        @Schema(description = "解析器目录")
        List<ParserCatalogItemResult> parsers,
        @Schema(description = "文档 Profile 模板目录")
        List<DocumentProfileCatalogItemResult> documentProfiles,
        @Schema(description = "三层配置说明")
        String configurationModelNoteZh
) {
}
