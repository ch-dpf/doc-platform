-- 分块配置档：文档级 chunk_profile_id + chunk metadata 冗余
ALTER TABLE doc_metadata
    ADD COLUMN IF NOT EXISTS chunk_profile_id VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_doc_chunk_profile
    ON doc_metadata(library_id, chunk_profile_id)
    WHERE deleted = FALSE AND chunk_profile_id IS NOT NULL;

-- 历史数据：请调用 POST /api/v1/vector-libraries/{libraryId}/chunk-profiles/backfill
-- 按文档 ingest_profile + 库/MIME 有效配置重算 cp_* 并同步 document_chunk.metadata.chunkProfileId
