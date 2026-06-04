-- Run once on existing docplatform DB (psql -f migrate-source-url.sql)
SET search_path TO ingest;

ALTER TABLE doc_metadata
    ADD COLUMN IF NOT EXISTS source_url VARCHAR(2048);

DROP INDEX IF EXISTS idx_doc_checksum_tenant;

CREATE UNIQUE INDEX IF NOT EXISTS idx_doc_checksum_upload_tenant
    ON doc_metadata (tenant_id, checksum_sha256)
    WHERE deleted = FALSE AND source_type = 'UPLOAD';

CREATE UNIQUE INDEX IF NOT EXISTS idx_doc_source_url_tenant
    ON doc_metadata (tenant_id, source_url)
    WHERE deleted = FALSE AND source_type = 'CRAWL' AND source_url IS NOT NULL;
