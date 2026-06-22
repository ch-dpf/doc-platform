CREATE TABLE kb_audit_event (
    event_id     UUID PRIMARY KEY,
    tenant_id    VARCHAR(64),
    actor_id     VARCHAR(128),
    event_type   VARCHAR(128) NOT NULL,
    subject_id   UUID,
    subject_type VARCHAR(64),
    trace_id     VARCHAR(128),
    payload_json JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_kb_audit_event_subject ON kb_audit_event (subject_type, subject_id);
CREATE INDEX idx_kb_audit_event_trace ON kb_audit_event (trace_id);
CREATE INDEX idx_kb_audit_event_created ON kb_audit_event (created_at);
