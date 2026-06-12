-- 批量任务失败文档 ID 列表（用于重试）
ALTER TABLE library_batch_job
    ADD COLUMN IF NOT EXISTS failed_doc_ids JSONB NOT NULL DEFAULT '[]'::jsonb;
