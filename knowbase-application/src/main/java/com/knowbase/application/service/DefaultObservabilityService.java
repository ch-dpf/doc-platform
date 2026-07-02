package com.knowbase.application.service;

import com.knowbase.api.result.PipelineSpanResult;
import com.knowbase.domain.repository.ObservabilityRepository;

import java.util.List;
import java.util.UUID;

public final class DefaultObservabilityService {

    private final ObservabilityRepository observabilityRepository;

    public DefaultObservabilityService(ObservabilityRepository observabilityRepository) {
        this.observabilityRepository = observabilityRepository;
    }

    public List<PipelineSpanResult> listTrace(UUID traceId) {
        return observabilityRepository.listPipelineSpans(traceId).stream()
                .map(span -> new PipelineSpanResult(
                        span.spanId(),
                        span.traceId(),
                        span.pipeline(),
                        span.runId(),
                        span.stage(),
                        span.status(),
                        span.durationMs(),
                        span.attributes(),
                        span.startedAt(),
                        span.finishedAt()
                ))
                .toList();
    }

    public List<PipelineSpanResult> listPipelineRun(String pipeline, UUID runId) {
        return observabilityRepository.listPipelineSpansByRun(pipeline, runId).stream()
                .map(span -> new PipelineSpanResult(
                        span.spanId(),
                        span.traceId(),
                        span.pipeline(),
                        span.runId(),
                        span.stage(),
                        span.status(),
                        span.durationMs(),
                        span.attributes(),
                        span.startedAt(),
                        span.finishedAt()
                ))
                .toList();
    }
}
