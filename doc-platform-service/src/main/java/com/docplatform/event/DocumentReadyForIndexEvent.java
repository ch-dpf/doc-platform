package com.docplatform.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record DocumentReadyForIndexEvent(
        @JsonProperty("eventType") DocumentEventType eventType,
        @JsonProperty("libraryId") UUID libraryId,
        @JsonProperty("docId") UUID docId,
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("version") int version,
        @JsonProperty("checksum") String checksum,
        @JsonProperty("mimeType") String mimeType,
        @JsonProperty("parsedTextUrl") String parsedTextUrl,
        @JsonProperty("parsedTextKey") String parsedTextKey,
        @JsonProperty("occurredAt") Instant occurredAt
) implements DocumentLifecycleEvent {

    @JsonCreator
    public DocumentReadyForIndexEvent {
        if (eventType == null) {
            eventType = DocumentEventType.DOCUMENT_READY_FOR_INDEX;
        }
    }

    public static DocumentReadyForIndexEvent create(
            UUID libraryId,
            UUID docId,
            String tenantId,
            int version,
            String checksum,
            String mimeType,
            String parsedTextUrl) {
        return create(libraryId, docId, tenantId, version, checksum, mimeType, parsedTextUrl, null);
    }

    public static DocumentReadyForIndexEvent create(
            UUID libraryId,
            UUID docId,
            String tenantId,
            int version,
            String checksum,
            String mimeType,
            String parsedTextUrl,
            String parsedTextKey) {
        return new DocumentReadyForIndexEvent(
                DocumentEventType.DOCUMENT_READY_FOR_INDEX,
                libraryId,
                docId,
                tenantId,
                version,
                checksum,
                mimeType,
                parsedTextUrl,
                parsedTextKey,
                Instant.now());
    }
}
