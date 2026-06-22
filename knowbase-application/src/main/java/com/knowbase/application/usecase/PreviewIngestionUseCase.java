package com.knowbase.application.usecase;

import com.knowbase.api.command.PreviewIngestionCommand;
import com.knowbase.api.result.IngestionPreviewResult;

/**
 * 入库预览用例：完整走解析 → 清洗 → 切块，不写入索引。
 * <p>
 * 与 {@link PrepareIngestionUseCase} 共用同一套解析路由（DocumentProfile + ParseOptionsSupport）。
 */
public interface PreviewIngestionUseCase {

    /**
     * 预览批量文档的解析结构块与切块结果。
     *
     * @param command 含 sourceUris 及 parseMode / parserCode 等解析选项
     * @return 含 {@link com.knowbase.api.result.ParseStageResult} 的文档级预览
     */
    IngestionPreviewResult preview(PreviewIngestionCommand command);
}
