package com.knowbase.event;

import java.util.UUID;

public final class IdempotencyKeys {

    public static String forEvent(UUID docId, int version, DocumentEventType eventType) {
        return docId + ":" + version + ":" + eventType.name();
    }

    private IdempotencyKeys() {
    }
}
