package com.knowbase.application.service;

import com.knowbase.domain.model.EvalRun;
import com.knowbase.domain.model.EvalSample;
import com.knowbase.domain.model.PipelineSpan;
import com.knowbase.domain.repository.ObservabilityRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryObservabilityRepository implements ObservabilityRepository {

    private final ConcurrentMap<UUID, PipelineSpan> spans = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, EvalRun> evalRuns = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<EvalSample>> evalSamples = new ConcurrentHashMap<>();

    @Override
    public PipelineSpan savePipelineSpan(PipelineSpan span) {
        spans.put(span.spanId(), span);
        return span;
    }

    @Override
    public List<PipelineSpan> listPipelineSpans(UUID traceId) {
        return spans.values().stream()
                .filter(span -> span.traceId().equals(traceId))
                .sorted((left, right) -> left.startedAt().compareTo(right.startedAt()))
                .toList();
    }

    @Override
    public List<PipelineSpan> listPipelineSpansByRun(String pipeline, UUID runId) {
        return spans.values().stream()
                .filter(span -> span.pipeline().equals(pipeline))
                .filter(span -> span.runId().equals(runId))
                .sorted((left, right) -> left.startedAt().compareTo(right.startedAt()))
                .toList();
    }

    @Override
    public EvalRun saveEvalRun(EvalRun evalRun) {
        evalRuns.put(evalRun.evalRunId(), evalRun);
        return evalRun;
    }

    @Override
    public Optional<EvalRun> findEvalRun(UUID evalRunId) {
        return Optional.ofNullable(evalRuns.get(evalRunId));
    }

    @Override
    public List<EvalRun> listEvalRuns(String tenantId, UUID agentId) {
        return evalRuns.values().stream()
                .filter(run -> tenantId.equals(run.tenantId()))
                .filter(run -> agentId == null || agentId.equals(run.agentId()))
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();
    }

    @Override
    public EvalSample saveEvalSample(EvalSample sample) {
        evalSamples.compute(sample.evalRunId(), (key, existing) -> {
            List<EvalSample> updated = new ArrayList<>(existing == null ? List.of() : existing);
            updated.add(sample);
            return List.copyOf(updated);
        });
        return sample;
    }

    @Override
    public List<EvalSample> listEvalSamples(UUID evalRunId) {
        return evalSamples.getOrDefault(evalRunId, List.of());
    }
}
