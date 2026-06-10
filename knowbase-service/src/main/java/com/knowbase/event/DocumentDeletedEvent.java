package com.knowbase.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record DocumentDeletedEvent(
        @JsonProperty("eventType") DocumentEventType eventType,
        @JsonProperty("docId") UUID docId,
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("version") int version,
        @JsonProperty("occurredAt") Instant occurredAt
) implements DocumentLifecycleEvent {

    @JsonCreator
    public DocumentDeletedEvent {
        if (eventType == null) {
            eventType = DocumentEventType.DOCUMENT_DELETED;
        }
    }

    public static DocumentDeletedEvent create(UUID docId, String tenantId, int version) {
        return new DocumentDeletedEvent(
                DocumentEventType.DOCUMENT_DELETED,
                docId,
                tenantId,
                version,
                Instant.now());
    }
}
