ALTER TABLE kb_retrieval_eval_run
    ADD COLUMN IF NOT EXISTS mrr DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS context_precision_at_k DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS stratified_recall_json JSONB NOT NULL DEFAULT '{}'::jsonb;
