CREATE TABLE kb_document_index_job (
    job_id         UUID PRIMARY KEY,
    run_id         UUID         NOT NULL REFERENCES kb_ingestion_run (run_id) ON DELETE CASCADE,
    library_id     UUID         NOT NULL REFERENCES kb_library (library_id) ON DELETE CASCADE,
    document_id    UUID,
    source_uri     TEXT         NOT NULL,
    status         VARCHAR(32)  NOT NULL,
    stage          VARCHAR(32)  NOT NULL,
    chunk_count    INT          NOT NULL DEFAULT 0,
    message        TEXT,
    error_message  TEXT,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_kb_document_index_job_run ON kb_document_index_job (run_id, created_at);
CREATE INDEX idx_kb_document_index_job_library ON kb_document_index_job (library_id, updated_at DESC);
