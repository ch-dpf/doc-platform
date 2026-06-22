package com.knowbase.api.result;

import java.util.Map;

/**
 * 解析阶段产出的单个结构块预览。
 * <p>
 * 对应 ingestion 层 {@code StructuralBlock}，blockType 常见值：heading、paragraph、tableRow。
 */
public record StructuralBlockResult(
        /** 文档内顺序序号，从 0 起 */
        int ordinal,
        /** 块类型：heading / paragraph / tableRow 等 */
        String blockType,
        /** 标题层级，非 heading 时为 0 */
        int level,
        /** 块内容预览，超出 maxPreviewChars 时截断 */
        String contentPreview,
        /** 块级元数据，如 headingPath、listStyle 等 */
        Map<String, Object> metadata
) {
}
