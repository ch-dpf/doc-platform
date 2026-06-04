package com.docplatform.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record DocumentIndexedEvent(
        @JsonProperty("eventType") DocumentEventType eventType,
        @JsonProperty("docId") UUID docId,
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("version") int version,
        @JsonProperty("chunkCount") int chunkCount,
        @JsonProperty("occurredAt") Instant occurredAt
) implements DocumentLifecycleEvent {

    @JsonCreator
    public DocumentIndexedEvent {
        if (eventType == null) {
            eventType = DocumentEventType.DOCUMENT_INDEXED;
        }
    }

    public static DocumentIndexedEvent create(UUID docId, String tenantId, int version, int chunkCount) {
        return new DocumentIndexedEvent(
                DocumentEventType.DOCUMENT_INDEXED,
                docId,
                tenantId,
                version,
                chunkCount,
                Instant.now());
    }
}
