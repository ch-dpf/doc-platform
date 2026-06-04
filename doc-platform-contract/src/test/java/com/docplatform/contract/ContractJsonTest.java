package com.docplatform.contract;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ContractJsonTest {

    @Test
    void roundTripReadyForIndex() {
        DocumentReadyForIndexEvent event = DocumentReadyForIndexEvent.create(
                UUID.randomUUID(),
                "tenant-a",
                1,
                "abc",
                "text/plain",
                "http://minio/parsed.txt");
        String json = ContractJson.write(event);
        DocumentLifecycleEvent parsed = ContractJson.read(json);
        assertInstanceOf(DocumentReadyForIndexEvent.class, parsed);
        assertEquals(event.docId(), parsed.docId());
        assertEquals(DocumentEventType.DOCUMENT_READY_FOR_INDEX, parsed.eventType());
    }

    @Test
    void idempotencyKeyStable() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String key = IdempotencyKeys.forEvent(id, 2, DocumentEventType.DOCUMENT_READY_FOR_INDEX);
        assertEquals(id + ":2:DOCUMENT_READY_FOR_INDEX", key);
    }
}
