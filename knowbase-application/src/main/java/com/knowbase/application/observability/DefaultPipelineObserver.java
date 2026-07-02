package com.knowbase.application.observability;

import com.knowbase.domain.model.IngestionDocumentError;
import com.knowbase.domain.model.PipelineSpan;
import com.knowbase.domain.observability.PipelineObserver;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.repository.ObservabilityRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultPipelineObserver implements PipelineObserver {

    private record SpanContext(UUID traceId, String pipeline, UUID runId, String stage, Instant startedAt, Map<String, Object> attributes) {
    }

    private final ObservabilityRepository observabilityRepository;
    private final KnowbaseRepository knowbaseRepository;
    private final Map<UUID, SpanContext> activeSpans = new ConcurrentHashMap<>();

    public DefaultPipelineObserver(
            ObservabilityRepository observabilityRepository,
            KnowbaseRepository knowbaseRepository
    ) {
        this.observabilityRepository = observabilityRepository;
        this.knowbaseRepository = knowbaseRepository;
    }

    @Override
    public UUID startSpan(String pipeline, UUID runId, String stage, Map<String, Object> attributes) {
        UUID spanId = UUID.randomUUID();
        UUID traceId = resolveTraceId(attributes);
        Instant now = Instant.now();
        Map<String, Object> attrs = attributes == null ? new HashMap<>() : new HashMap<>(attributes);
        attrs.putIfAbsent("traceId", traceId.toString());
        SpanContext context = new SpanContext(traceId, pipeline, runId, stage, now, Map.copyOf(attrs));
        activeSpans.put(spanId, context);
        observabilityRepository.savePipelineSpan(new PipelineSpan(
                spanId,
                traceId,
                pipeline,
                runId,
                stage,
                "STARTED",
                null,
                context.attributes(),
                now,
                null
        ));
        return spanId;
    }

    @Override
    public void finishSpan(UUID spanId, String status, Map<String, Object> attributes) {
        SpanContext context = activeSpans.remove(spanId);
        if (context == null) {
            return;
        }
        Instant finished = Instant.now();
        long durationMs = Math.max(0, finished.toEpochMilli() - context.startedAt().toEpochMilli());
        Map<String, Object> merged = new HashMap<>(context.attributes());
        if (attributes != null) {
            merged.putAll(attributes);
        }
        observabilityRepository.savePipelineSpan(new PipelineSpan(
                spanId,
                context.traceId(),
                context.pipeline(),
                context.runId(),
                context.stage(),
                status,
                durationMs,
                Map.copyOf(merged),
                context.startedAt(),
                finished
        ));
    }

    @Override
    public void recordIngestionError(UUID runId, String sourceUri, String errorCode, String message) {
        knowbaseRepository.saveIngestionDocumentError(new IngestionDocumentError(
                UUID.randomUUID(),
                runId,
                sourceUri,
                errorCode,
                message,
                Instant.now()
        ));
    }

    private static UUID resolveTraceId(Map<String, Object> attributes) {
        if (attributes == null) {
            return UUID.randomUUID();
        }
        Object traceId = attributes.get("traceId");
        if (traceId instanceof UUID uuid) {
            return uuid;
        }
        if (traceId != null) {
            try {
                return UUID.fromString(String.valueOf(traceId));
            } catch (IllegalArgumentException ignored) {
                return UUID.randomUUID();
            }
        }
        return UUID.randomUUID();
    }
}
