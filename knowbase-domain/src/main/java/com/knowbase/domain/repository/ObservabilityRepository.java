package com.knowbase.domain.repository;

import com.knowbase.domain.model.EvalRun;
import com.knowbase.domain.model.EvalSample;
import com.knowbase.domain.model.PipelineSpan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObservabilityRepository {

    PipelineSpan savePipelineSpan(PipelineSpan span);

    List<PipelineSpan> listPipelineSpans(UUID traceId);

    List<PipelineSpan> listPipelineSpansByRun(String pipeline, UUID runId);

    EvalRun saveEvalRun(EvalRun evalRun);

    Optional<EvalRun> findEvalRun(UUID evalRunId);

    List<EvalRun> listEvalRuns(String tenantId, UUID agentId);

    EvalSample saveEvalSample(EvalSample sample);

    List<EvalSample> listEvalSamples(UUID evalRunId);
}
