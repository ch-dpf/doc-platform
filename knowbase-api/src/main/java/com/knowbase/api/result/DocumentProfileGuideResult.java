package com.knowbase.api.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "库预设内某 Document Profile 的产品说明")
public record DocumentProfileGuideResult(
        String code,
        String nameZh,
        String descriptionZh,
        String contentFamily,
        String parserCode,
        String parserNameZh,
        boolean parserBuiltIn,
        boolean parserExternal,
        String chunkingStrategy,
        String chunkingStrategyLabelZh,
        List<String> fileExtensions
) {
}
