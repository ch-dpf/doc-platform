package com.knowbase.application.service;

import com.knowbase.domain.model.IngestionRun;
import com.knowbase.ingestion.IngestionPipeline;
import com.knowbase.ingestion.IngestionRequest;

public final class SynchronousIngestionRunExecutor implements IngestionRunExecutor {

    private final IngestionPipeline ingestionPipeline;
    private final IngestionEvalDraftService evalDraftService;

    public SynchronousIngestionRunExecutor(IngestionPipeline ingestionPipeline) {
        this(ingestionPipeline, null);
    }

    public SynchronousIngestionRunExecutor(
            IngestionPipeline ingestionPipeline,
            IngestionEvalDraftService evalDraftService
    ) {
        this.ingestionPipeline = ingestionPipeline;
        this.evalDraftService = evalDraftService;
    }

    @Override
    public IngestionRun execute(IngestionRequest request) {
        IngestionRun completed = ingestionPipeline.run(request);
        maybeGenerateEvalDrafts(completed);
        return completed;
    }

    private void maybeGenerateEvalDrafts(IngestionRun completed) {
        if (evalDraftService == null) {
            return;
        }
        try {
            evalDraftService.onRunCompleted(completed);
        } catch (RuntimeException ignored) {
            // 评测草稿生成失败不应阻断入库主流程。
        }
    }
}
