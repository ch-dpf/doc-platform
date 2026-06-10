-- 文档级自定义 metadata（供检索 metadataFilterFields 过滤，如 department、docType）
ALTER TABLE doc_metadata
    ADD COLUMN IF NOT EXISTS custom_metadata JSONB;
