package com.knowbase.application.service;

import com.knowbase.domain.audit.AuditEvent;
import com.knowbase.domain.audit.AuditSink;
import com.knowbase.domain.model.IngestionRun;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.status.IngestionRunStatus;
import com.knowbase.ingestion.IngestionPipeline;
import com.knowbase.ingestion.IngestionRequest;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executor;

public final class AsyncIngestionRunExecutor implements IngestionRunExecutor {

    private final KnowbaseRepository repository;
    private final IngestionPipeline ingestionPipeline;
    private final AuditSink auditSink;
    private final Executor executor;
    private final IngestionEvalDraftService evalDraftService;

    public AsyncIngestionRunExecutor(
            KnowbaseRepository repository,
            IngestionPipeline ingestionPipeline,
            AuditSink auditSink,
            Executor executor
    ) {
        this(repository, ingestionPipeline, auditSink, executor, null);
    }

    public AsyncIngestionRunExecutor(
            KnowbaseRepository repository,
            IngestionPipeline ingestionPipeline,
            AuditSink auditSink,
            Executor executor,
            IngestionEvalDraftService evalDraftService
    ) {
        this.repository = repository;
        this.ingestionPipeline = ingestionPipeline;
        this.auditSink = auditSink;
        this.executor = executor;
        this.evalDraftService = evalDraftService;
    }

    @Override
    public IngestionRun execute(IngestionRequest request) {
        IngestionRun queued = repository.findIngestionRun(request.runId())
                .orElseThrow(() -> new ResourceNotFoundException("入库运行不存在: " + request.runId()));
        executor.execute(() -> runPipeline(request));
        return queued;
    }

    private void runPipeline(IngestionRequest request) {
        try {
            IngestionRun completed = ingestionPipeline.run(request);
            maybeGenerateEvalDrafts(completed);
            auditSink.record(AuditEvent.now(
                    null,
                    null,
                    "INGESTION_RUN_COMPLETED",
                    request.runId(),
                    "IngestionRun",
                    request.runId().toString(),
                    Map.of("status", completed.status().name(), "chunkCount", completed.chunkCount())
            ));
        } catch (RuntimeException exception) {
            IngestionRun existing = repository.findIngestionRun(request.runId()).orElse(null);
            if (existing != null) {
                repository.saveIngestionRun(new IngestionRun(
                        existing.runId(),
                        existing.libraryId(),
                        IngestionRunStatus.FAILED,
                        existing.sourceUris(),
                        existing.sourceType(),
                        existing.documentProfileCode(),
                        existing.publishIndexOnSuccess(),
                        existing.inputDocuments(),
                        existing.succeededDocuments(),
                        existing.failedDocuments(),
                        existing.chunkCount(),
                        existing.indexVersionId(),
                        "异步入库执行失败：" + failureMessage(exception),
                        existing.options(),
                        existing.createdAt(),
                        Instant.now()
                ));
            }
            auditSink.record(AuditEvent.now(
                    null,
                    null,
                    "INGESTION_RUN_FAILED",
                    request.runId(),
                    "IngestionRun",
                    request.runId().toString(),
                    Map.of("error", failureMessage(exception))
            ));
        }
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

    private static String failureMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
