package com.knowbase.application.usecase;

import com.knowbase.api.command.PrepareIngestionCommand;
import com.knowbase.api.result.IngestionPrepareResult;

/**
 * 入库准备用例：在向量化前分阶段执行解析、清洗与切块，供调试与 UI 预览。
 * <p>
 * 解析层入口为 {@link com.knowbase.ingestion.DocumentPreparationPipeline#parse}，
 * 由 {@link com.knowbase.application.service.DefaultIngestionPrepareService} 调用并映射为 {@link com.knowbase.api.result.ParseStageResult}。
 */
public interface PrepareIngestionUseCase {

    /**
     * 按 prepareStage 执行单文档或多文档准备流水线。
     *
     * @param command 含 sourceUris、documentProfileCode、prepareStage 及 parseMode 等 options
     * @return 每文档的 parse / normalize / chunk 阶段快照
     */
    IngestionPrepareResult prepare(PrepareIngestionCommand command);
}
