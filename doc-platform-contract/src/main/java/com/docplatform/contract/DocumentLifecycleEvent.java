package com.docplatform.contract;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DocumentReadyForIndexEvent.class, name = "DOCUMENT_READY_FOR_INDEX"),
        @JsonSubTypes.Type(value = DocumentDeletedEvent.class, name = "DOCUMENT_DELETED"),
        @JsonSubTypes.Type(value = DocumentIndexedEvent.class, name = "DOCUMENT_INDEXED")
})
public sealed interface DocumentLifecycleEvent permits
        DocumentReadyForIndexEvent, DocumentDeletedEvent, DocumentIndexedEvent {

    DocumentEventType eventType();

    UUID docId();

    String tenantId();

    int version();

    Instant occurredAt();
}
