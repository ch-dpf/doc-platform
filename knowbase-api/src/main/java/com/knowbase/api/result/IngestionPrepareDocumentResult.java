package com.knowbase.api.result;

/**
 * 单文档入库准备结果，按 prepareStage 填充对应阶段字段。
 * <p>
 * 当 prepareStage=parse 时，{@link #parse} 有值而 normalize / chunk 为 null；
 * 失败时仅 {@link #error} 有值。
 */
public record IngestionPrepareDocumentResult(
        String sourceUri,
        String title,
        String documentProfileCode,
        String contentFamily,
        /** 实际执行阶段：parse | normalize | document_summary | chunk | all */
        String prepareStage,
        /** 解析阶段结果；prepareStage 含 parse 时非空 */
        ParseStageResult parse,
        NormalizeStageResult normalize,
        DocumentSummaryStageResult documentSummary,
        ChunkStageResult chunk,
        /** 后处理阶段结果；prepareStage 含 post_process 时非空 */
        PostProcessStageResult postProcess,
        /** 面向产品预览的质量洞察：评分、风险、建议与关键指标 */
        IngestionQualityInsightResult qualityInsight,
        String error
) {
}
