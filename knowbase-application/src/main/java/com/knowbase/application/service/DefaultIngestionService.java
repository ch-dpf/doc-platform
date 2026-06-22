package com.knowbase.application.service;

import com.knowbase.api.command.CreateIngestionRunCommand;
import com.knowbase.api.facade.KnowbaseIngestionFacade;
import com.knowbase.api.result.IngestionRunResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.usecase.RunIngestionUseCase;
import com.knowbase.domain.audit.AuditEvent;
import com.knowbase.domain.audit.AuditSink;
import com.knowbase.domain.model.IngestionRun;
import com.knowbase.domain.model.KnowledgeLibrary;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.status.IngestionRunStatus;
import com.knowbase.ingestion.IngestionRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DefaultIngestionService implements RunIngestionUseCase, KnowbaseIngestionFacade {

    private final KnowbaseRepository repository;
    private final IngestionRunExecutor ingestionRunExecutor;
    private final AuditSink auditSink;

    public DefaultIngestionService(KnowbaseRepository repository, IngestionRunExecutor ingestionRunExecutor) {
        this(repository, ingestionRunExecutor, event -> {
        });
    }

    public DefaultIngestionService(
            KnowbaseRepository repository,
            IngestionRunExecutor ingestionRunExecutor,
            AuditSink auditSink
    ) {
        this.repository = repository;
        this.ingestionRunExecutor = ingestionRunExecutor;
        this.auditSink = auditSink;
    }

    @Override
    public IngestionRunResult create(CreateIngestionRunCommand command) {
        KnowledgeLibrary library = repository.findLibrary(command.libraryId())
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + command.libraryId()));

        Instant now = Instant.now();
        UUID runId = UUID.randomUUID();
        IngestionRun created = new IngestionRun(
                runId,
                command.libraryId(),
                IngestionRunStatus.CREATED,
                List.copyOf(command.sourceUris()),
                command.sourceType(),
                command.documentProfileCode(),
                command.publishIndexOnSuccess(),
                command.sourceUris().size(),
                0,
                0,
                0,
                null,
                "入库运行已创建，等待 Pipeline 执行",
                command.options() == null ? Map.of() : command.options(),
                now,
                now
        );
        repository.saveIngestionRun(created);
        auditSink.record(AuditEvent.now(
                library.tenantId(),
                library.tenantId(),
                "INGESTION_RUN_CREATED",
                runId,
                "IngestionRun",
                runId.toString(),
                Map.of("libraryId", command.libraryId().toString(), "sourceCount", command.sourceUris().size())
        ));

        IngestionRun result = ingestionRunExecutor.execute(new IngestionRequest(
                runId,
                command.libraryId(),
                command.sourceUris(),
                command.documentProfileCode(),
                command.publishIndexOnSuccess(),
                command.options() == null ? Map.of() : command.options()
        ));
        if (isTerminal(result.status())) {
            auditSink.record(AuditEvent.now(
                    null,
                    null,
                    "INGESTION_RUN_COMPLETED",
                    runId,
                    "IngestionRun",
                    runId.toString(),
                    Map.of("status", result.status().name(), "chunkCount", result.chunkCount())
            ));
        }
        return ResultMapper.toIngestionRunResult(result);
    }

    @Override
    public IngestionRunResult get(UUID runId) {
        return repository.findIngestionRun(runId)
                .map(ResultMapper::toIngestionRunResult)
                .orElseThrow(() -> new ResourceNotFoundException("入库运行不存在: " + runId));
    }

    @Override
    public IngestionRunResult createIngestionRun(CreateIngestionRunCommand command) {
        return create(command);
    }

    @Override
    public IngestionRunResult getIngestionRun(UUID runId) {
        return get(runId);
    }

    private static boolean isTerminal(IngestionRunStatus status) {
        return status == IngestionRunStatus.SUCCEEDED
                || status == IngestionRunStatus.PARTIAL_FAILED
                || status == IngestionRunStatus.FAILED
                || status == IngestionRunStatus.CANCELLED;
    }
}
