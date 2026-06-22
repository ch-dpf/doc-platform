-- ACL
CREATE TABLE kb_acl_entry (
    acl_id          UUID PRIMARY KEY,
    tenant_id       VARCHAR(64)  NOT NULL,
    resource_type   VARCHAR(32)  NOT NULL,
    resource_id     UUID         NOT NULL,
    principal_type  VARCHAR(16)  NOT NULL,
    principal_id    VARCHAR(128) NOT NULL,
    permission      VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_kb_acl_resource ON kb_acl_entry (tenant_id, resource_type, resource_id);
CREATE INDEX idx_kb_acl_principal ON kb_acl_entry (tenant_id, principal_type, principal_id);

-- Ingestion per-document errors
CREATE TABLE kb_ingestion_document_error (
    error_id        UUID PRIMARY KEY,
    run_id          UUID         NOT NULL REFERENCES kb_ingestion_run (run_id),
    source_uri      TEXT         NOT NULL,
    error_code      VARCHAR(64),
    error_message   TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_kb_ingestion_error_run ON kb_ingestion_document_error (run_id);

-- Pipeline tracing
CREATE TABLE kb_pipeline_span (
    span_id         UUID PRIMARY KEY,
    trace_id        UUID         NOT NULL,
    pipeline        VARCHAR(64)  NOT NULL,
    run_id          UUID         NOT NULL,
    stage           VARCHAR(64)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    duration_ms     BIGINT,
    attributes_json JSONB        NOT NULL DEFAULT '{}'::jsonb,
    started_at      TIMESTAMPTZ  NOT NULL,
    finished_at     TIMESTAMPTZ
);

CREATE INDEX idx_kb_pipeline_span_trace ON kb_pipeline_span (trace_id);
CREATE INDEX idx_kb_pipeline_span_run ON kb_pipeline_span (pipeline, run_id);

-- Evaluation runs
CREATE TABLE kb_eval_run (
    eval_run_id     UUID PRIMARY KEY,
    tenant_id       VARCHAR(64)  NOT NULL,
    agent_id        UUID,
    eval_type       VARCHAR(64)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    metrics_json    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ  NOT NULL,
    finished_at     TIMESTAMPTZ
);

CREATE TABLE kb_eval_sample (
    sample_id       UUID PRIMARY KEY,
    eval_run_id     UUID         NOT NULL REFERENCES kb_eval_run (eval_run_id),
    question        TEXT         NOT NULL,
    expected_answer TEXT,
    actual_answer   TEXT,
    score           DOUBLE PRECISION,
    metrics_json    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_kb_eval_sample_run ON kb_eval_sample (eval_run_id);
