package com.knowbase.api.result;

import java.util.List;
import java.util.Map;

/**
 * 入库解析阶段的输出快照，供 prepare / preview 接口返回。
 * <p>
 * 由业务层将 {@code knowbase-ingestion} 模块的 {@code ParsedDocument} 裁剪后映射而来，
 * 不包含完整原文，仅保留统计信息与可预览片段。
 */
public record ParseStageResult(
        /** 实际使用的解析器编码，如 docx-structure、pdf-layout、qa、tika */
        String parserCode,
        /** 是否产出结构块（heading / paragraph / tableRow 等），结构感知解析为 true */
        boolean structureAware,
        /** 结构块总数（非预览块数） */
        int blockCount,
        /** 扁平化全文总字符数 */
        int textCharCount,
        /** 全文预览，超出 maxPreviewChars 时截断并追加 "..." */
        String textPreview,
        /** 结构块预览列表，数量受 maxPreviewBlocks 限制 */
        List<StructuralBlockResult> blocks,
        /** 解析器附加元数据，如 qaPairCount、zipEntryCount、structureAware 等 */
        Map<String, Object> metadata
) {
}
