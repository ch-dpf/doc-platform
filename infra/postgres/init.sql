CREATE EXTENSION IF NOT EXISTS vector;



CREATE SCHEMA IF NOT EXISTS ingest;

CREATE SCHEMA IF NOT EXISTS vector_idx;



-- ingest schema (also managed by JPA ddl-auto for dev)

SET search_path TO ingest;



CREATE TABLE IF NOT EXISTS doc_metadata (

    doc_id           UUID PRIMARY KEY,

    tenant_id        VARCHAR(64) NOT NULL,

    source_type      VARCHAR(32) NOT NULL,

    file_name        VARCHAR(512) NOT NULL,

    mime_type        VARCHAR(128),

    size_bytes       BIGINT NOT NULL,

    storage_key      VARCHAR(1024) NOT NULL,

    source_url       VARCHAR(2048),

    checksum_sha256  VARCHAR(64) NOT NULL,

    parse_status     VARCHAR(32) NOT NULL,

    parsed_text_key  VARCHAR(1024),

    version          INT NOT NULL DEFAULT 1,

    index_requested  BOOLEAN NOT NULL DEFAULT TRUE,

    index_status     VARCHAR(32),

    deleted          BOOLEAN NOT NULL DEFAULT FALSE,

    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()

);



CREATE INDEX IF NOT EXISTS idx_doc_tenant ON doc_metadata(tenant_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_doc_checksum_upload_tenant

    ON doc_metadata(tenant_id, checksum_sha256)

    WHERE deleted = FALSE AND source_type = 'UPLOAD';

CREATE UNIQUE INDEX IF NOT EXISTS idx_doc_source_url_tenant

    ON doc_metadata(tenant_id, source_url)

    WHERE deleted = FALSE AND source_type = 'CRAWL' AND source_url IS NOT NULL;



-- vector schema

SET search_path TO vector_idx, public;



CREATE TABLE IF NOT EXISTS processed_event (

    idempotency_key VARCHAR(256) PRIMARY KEY,

    processed_at    TIMESTAMPTZ NOT NULL DEFAULT now()

);



CREATE TABLE IF NOT EXISTS document_index_job (

    job_id         UUID PRIMARY KEY,

    doc_id         UUID NOT NULL,

    tenant_id      VARCHAR(64) NOT NULL,

    version        INT NOT NULL,

    status         VARCHAR(32) NOT NULL,

    error_message  TEXT,

    retry_count    INT NOT NULL DEFAULT 0,

    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    completed_at   TIMESTAMPTZ,

    UNIQUE (doc_id, version)

);



CREATE TABLE IF NOT EXISTS document_chunk (

    chunk_id       UUID PRIMARY KEY,

    doc_id         UUID NOT NULL,

    tenant_id      VARCHAR(64) NOT NULL,

    version        INT NOT NULL,

    chunk_index    INT NOT NULL,

    content        TEXT NOT NULL,

    metadata       JSONB,

    embedding      vector(768) NOT NULL,

    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()

);



CREATE INDEX IF NOT EXISTS idx_chunk_tenant_doc ON document_chunk(tenant_id, doc_id);

CREATE INDEX IF NOT EXISTS idx_chunk_embedding ON document_chunk

    USING hnsw (embedding vector_cosine_ops);

