package com.knowbase.api.result;

import java.util.List;

/**
 * 单文档入库预览结果，包含解析、清洗与切块三阶段快照。
 * <p>
 * {@link #parserCode} 为解析器快捷字段，与 {@link #parse#parserCode()} 一致。
 */
public record DocumentPreviewResult(
        String sourceUri,
        String title,
        String documentProfileCode,
        /** 解析器编码，取自 ParsedDocument.metadata.parserCode */
        String parserCode,
        String contentFamily,
        int chunkCount,
        int indexableChunkCount,
        List<ChunkPreviewResult> chunks,
        /** 解析阶段完整快照（含结构块预览） */
        ParseStageResult parse,
        NormalizeStageResult normalize,
        PostProcessStageResult postProcess,
        String error
) {
}
