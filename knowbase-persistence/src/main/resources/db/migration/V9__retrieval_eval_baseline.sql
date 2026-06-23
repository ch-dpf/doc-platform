CREATE TABLE kb_retrieval_eval_baseline (
    library_id              UUID PRIMARY KEY REFERENCES kb_library (library_id) ON DELETE CASCADE,
    eval_run_id             UUID         NOT NULL REFERENCES kb_retrieval_eval_run (eval_run_id) ON DELETE CASCADE,
    profile_id              UUID,
    index_generation_id     UUID,
    recall_at_k             DOUBLE PRECISION NOT NULL,
    hit_k                   INT          NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_kb_retrieval_eval_baseline_run ON kb_retrieval_eval_baseline (eval_run_id);
