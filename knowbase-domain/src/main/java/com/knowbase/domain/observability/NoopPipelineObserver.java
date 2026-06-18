package com.knowbase.domain.observability;

import java.util.Map;
import java.util.UUID;

public final class NoopPipelineObserver implements PipelineObserver {

    @Override
    public UUID startSpan(String pipeline, UUID runId, String stage, Map<String, Object> attributes) {
        return UUID.randomUUID();
    }

    @Override
    public void finishSpan(UUID spanId, String status, Map<String, Object> attributes) {
    }

    @Override
    public void recordIngestionError(UUID runId, String sourceUri, String errorCode, String message) {
    }
}
