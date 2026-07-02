package com.knowbase.persistence.store;

import com.knowbase.domain.audit.AuditEvent;
import com.knowbase.domain.audit.AuditSink;
import com.knowbase.persistence.support.JsonSupport;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;

public final class AuditEventStore implements AuditSink {

    private final JdbcTemplate jdbcTemplate;

    public AuditEventStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(AuditEvent event) {
        jdbcTemplate.update(
                """
                        INSERT INTO kb_audit_event (
                            event_id,
                            tenant_id,
                            actor_id,
                            event_type,
                            subject_id,
                            subject_type,
                            trace_id,
                            payload_json,
                            created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                        """,
                event.eventId(),
                event.tenantId(),
                event.actorId(),
                event.eventType(),
                event.subjectId(),
                event.subjectType(),
                event.traceId(),
                JsonSupport.write(event.payload()),
                Timestamp.from(event.createdAt())
        );
    }
}
