package com.knowbase.api.result;

import java.util.List;
import java.util.UUID;

/** 批量文档入库准备响应，documents 中每项可独立查看解析 / 清洗 / 切块结果。 */
public record IngestionPrepareResult(
        UUID libraryId,
        /** 本次请求指定的准备阶段 */
        String prepareStage,
        int sourceCount,
        int succeeded,
        int failed,
        /** 批次级质量洞察，聚合 documents 中的问题与建议 */
        IngestionQualityInsightResult qualityInsight,
        List<IngestionPrepareDocumentResult> documents
) {
}
