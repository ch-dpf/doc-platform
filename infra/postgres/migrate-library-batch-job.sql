-- 库级批量任务：重索引 / 分块档归档进度追踪
CREATE TABLE IF NOT EXISTS library_batch_job (
    job_id             UUID PRIMARY KEY,
    library_id         UUID NOT NULL REFERENCES vector_library(library_id) ON DELETE CASCADE,
    tenant_id          VARCHAR(64) NOT NULL,
    job_type           VARCHAR(32) NOT NULL,
    chunk_profile_id   VARCHAR(32),
    status             VARCHAR(32) NOT NULL,
    total_count        INT NOT NULL DEFAULT 0,
    completed_count    INT NOT NULL DEFAULT 0,
    failed_count       INT NOT NULL DEFAULT 0,
    last_error         TEXT,
    failed_doc_ids     JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_library_batch_job_library
    ON library_batch_job(library_id, created_at DESC);
