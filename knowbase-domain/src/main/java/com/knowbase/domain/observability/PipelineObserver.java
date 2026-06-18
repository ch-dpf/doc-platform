package com.knowbase.domain.observability;

import com.knowbase.domain.model.IngestionDocumentError;

import java.util.Map;
import java.util.UUID;

public interface PipelineObserver {

    UUID startSpan(String pipeline, UUID runId, String stage, Map<String, Object> attributes);

    void finishSpan(UUID spanId, String status, Map<String, Object> attributes);

    void recordIngestionError(UUID runId, String sourceUri, String errorCode, String message);
}
