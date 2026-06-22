package com.knowbase.application.service;

import com.knowbase.api.command.AskQuestionCommand;
import com.knowbase.api.facade.KnowbaseQuestionFacade;
import com.knowbase.api.result.QueryRunResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.pipeline.DefaultQueryPipeline;
import com.knowbase.application.usecase.AskQuestionUseCase;
import com.knowbase.domain.audit.AuditEvent;
import com.knowbase.domain.audit.AuditSink;
import com.knowbase.domain.model.KnowledgeAgent;
import com.knowbase.domain.model.QueryRun;
import com.knowbase.domain.repository.KnowbaseRepository;

import java.util.UUID;

public class DefaultQuestionService implements AskQuestionUseCase, KnowbaseQuestionFacade {

    private final KnowbaseRepository repository;
    private final DefaultQueryPipeline queryPipeline;
    private final AuditSink auditSink;

    public DefaultQuestionService(KnowbaseRepository repository, DefaultQueryPipeline queryPipeline) {
        this(repository, queryPipeline, event -> {
        });
    }

    public DefaultQuestionService(KnowbaseRepository repository, DefaultQueryPipeline queryPipeline, AuditSink auditSink) {
        this.repository = repository;
        this.queryPipeline = queryPipeline;
        this.auditSink = auditSink;
    }

    @Override
    public QueryRunResult ask(AskQuestionCommand command) {
        KnowledgeAgent agent = repository.findAgent(command.agentId())
                .orElseThrow(() -> new ResourceNotFoundException("知识智能体不存在: " + command.agentId()));
        UUID queryRunId = UUID.randomUUID();
        QueryRun completed = queryPipeline.run(
                queryRunId,
                agent.agentId(),
                command.agentVersionId(),
                command.question(),
                command.debugLibraryIds()
        );
        auditSink.record(AuditEvent.now(
                agent.tenantId(),
                null,
                "QUERY_RUN_COMPLETED",
                queryRunId,
                "QueryRun",
                completed.traceId(),
                java.util.Map.of(
                        "agentId", agent.agentId().toString(),
                        "status", completed.status().name(),
                        "evidenceCount", completed.evidencePack() == null ? 0 : completed.evidencePack().segments().size()
                )
        ));
        return ResultMapper.toQueryRunResult(completed);
    }

    @Override
    public QueryRunResult get(UUID queryRunId) {
        return repository.findQueryRun(queryRunId)
                .map(ResultMapper::toQueryRunResult)
                .orElseThrow(() -> new ResourceNotFoundException("问答运行不存在: " + queryRunId));
    }

    @Override
    public QueryRunResult getQueryRun(UUID queryRunId) {
        return get(queryRunId);
    }
}
