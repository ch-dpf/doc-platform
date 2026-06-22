package com.knowbase.domain.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(
        UUID eventId,
        String tenantId,
        String actorId,
        String eventType,
        UUID subjectId,
        String subjectType,
        String traceId,
        Map<String, Object> payload,
        Instant createdAt
) {

    public static AuditEvent now(
            String tenantId,
            String actorId,
            String eventType,
            UUID subjectId,
            String subjectType,
            String traceId,
            Map<String, Object> payload
    ) {
        return new AuditEvent(
                UUID.randomUUID(),
                tenantId,
                actorId,
                eventType,
                subjectId,
                subjectType,
                traceId,
                payload == null ? Map.of() : Map.copyOf(payload),
                Instant.now()
        );
    }
}
