CREATE TABLE kb_retrieval_eval_sample (
    sample_id               UUID PRIMARY KEY,
    library_id              UUID         NOT NULL REFERENCES kb_library (library_id) ON DELETE CASCADE,
    question                TEXT         NOT NULL,
    expected_document_ids   JSONB        NOT NULL DEFAULT '[]'::jsonb,
    expected_source_uris    JSONB        NOT NULL DEFAULT '[]'::jsonb,
    ground_truth_contexts   JSONB        NOT NULL DEFAULT '[]'::jsonb,
    hit_rank                INT          NOT NULL DEFAULT 8,
    notes                   TEXT,
    enabled                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_retrieval_eval_sample_criteria CHECK (
        jsonb_array_length(expected_document_ids) > 0
        OR jsonb_array_length(expected_source_uris) > 0
        OR jsonb_array_length(ground_truth_contexts) > 0
    )
);

CREATE INDEX idx_kb_retrieval_eval_sample_library ON kb_retrieval_eval_sample (library_id, enabled, updated_at DESC);

CREATE TABLE kb_retrieval_eval_run (
    eval_run_id             UUID PRIMARY KEY,
    library_id              UUID         NOT NULL REFERENCES kb_library (library_id) ON DELETE CASCADE,
    status                  VARCHAR(32)  NOT NULL,
    hit_k                   INT          NOT NULL DEFAULT 8,
    total_samples           INT          NOT NULL DEFAULT 0,
    passed_samples          INT          NOT NULL DEFAULT 0,
    recall_at_k             DOUBLE PRECISION,
    retrieval_policy_json   JSONB        NOT NULL DEFAULT '{}'::jsonb,
    message                 TEXT,
    created_at              TIMESTAMPTZ  NOT NULL,
    completed_at            TIMESTAMPTZ
);

CREATE INDEX idx_kb_retrieval_eval_run_library ON kb_retrieval_eval_run (library_id, created_at DESC);

CREATE TABLE kb_retrieval_eval_result (
    result_id               UUID PRIMARY KEY,
    eval_run_id             UUID         NOT NULL REFERENCES kb_retrieval_eval_run (eval_run_id) ON DELETE CASCADE,
    sample_id               UUID         NOT NULL REFERENCES kb_retrieval_eval_sample (sample_id) ON DELETE CASCADE,
    question                TEXT         NOT NULL,
    hit                     BOOLEAN      NOT NULL,
    hit_rank_used           INT          NOT NULL,
    first_hit_rank          INT,
    matched_document_id     UUID,
    matched_chunk_id        UUID,
    match_type              VARCHAR(64),
    retrieved_count         INT          NOT NULL DEFAULT 0,
    failure_reason          TEXT,
    trace_json              JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at              TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_kb_retrieval_eval_result_run ON kb_retrieval_eval_result (eval_run_id, created_at);
