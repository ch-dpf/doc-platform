CREATE EXTENSION IF NOT EXISTS vector;

SET client_encoding TO 'UTF8';
SET search_path TO public;

-- 向量库（逻辑知识库）
CREATE TABLE IF NOT EXISTS vector_library (
    library_id       UUID PRIMARY KEY,
    tenant_id        VARCHAR(64) NOT NULL,
    name             VARCHAR(256) NOT NULL,
    description      TEXT,
    status           VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    config_json      JSONB NOT NULL DEFAULT '{}',
    document_count   INT NOT NULL DEFAULT 0,
    chunk_count      INT NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_library_tenant ON vector_library(tenant_id);

-- 异步上传任务（大文件）
CREATE TABLE IF NOT EXISTS upload_task (
    task_id          UUID PRIMARY KEY,
    library_id       UUID NOT NULL REFERENCES vector_library(library_id) ON DELETE CASCADE,
    tenant_id        VARCHAR(64) NOT NULL,
    file_name        VARCHAR(512) NOT NULL,
    status           VARCHAR(32) NOT NULL,
    progress         INT NOT NULL DEFAULT 0,
    doc_id           UUID,
    error_message    TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_upload_task_library ON upload_task(library_id);

-- 文档元数据
CREATE TABLE IF NOT EXISTS doc_metadata (
    doc_id           UUID PRIMARY KEY,
    library_id       UUID NOT NULL REFERENCES vector_library(library_id),
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
    custom_metadata  JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_doc_library ON doc_metadata(library_id);
CREATE INDEX IF NOT EXISTS idx_doc_tenant ON doc_metadata(tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_doc_checksum_library_tenant
    ON doc_metadata(library_id, tenant_id, checksum_sha256)
    WHERE deleted = FALSE AND source_type = 'UPLOAD';
CREATE UNIQUE INDEX IF NOT EXISTS idx_doc_source_url_library_tenant
    ON doc_metadata(library_id, tenant_id, source_url)
    WHERE deleted = FALSE AND source_type = 'CRAWL' AND source_url IS NOT NULL;
-- 向量索引
CREATE TABLE IF NOT EXISTS processed_event (
    idempotency_key VARCHAR(256) PRIMARY KEY,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS document_index_job (
    job_id         UUID PRIMARY KEY,
    library_id     UUID NOT NULL,
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
    library_id     UUID NOT NULL,
    doc_id         UUID NOT NULL,
    tenant_id      VARCHAR(64) NOT NULL,
    version        INT NOT NULL,
    chunk_index    INT NOT NULL,
    content        TEXT NOT NULL,
    metadata       JSONB,
    embedding      vector(768) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_chunk_library ON document_chunk(library_id);
CREATE INDEX IF NOT EXISTS idx_chunk_tenant_doc ON document_chunk(tenant_id, doc_id);
CREATE INDEX IF NOT EXISTS idx_chunk_embedding ON document_chunk
    USING hnsw (embedding vector_cosine_ops);

-- 对话会话（持久化上下文记忆）
CREATE TABLE IF NOT EXISTS chat_conversation (
    conversation_id  UUID PRIMARY KEY,
    library_id       UUID NOT NULL REFERENCES vector_library(library_id) ON DELETE CASCADE,
    tenant_id        VARCHAR(64) NOT NULL,
    title            VARCHAR(256),
    summary          TEXT,
    message_count    INT NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted          BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_chat_conv_library ON chat_conversation(library_id);
CREATE INDEX IF NOT EXISTS idx_chat_conv_tenant ON chat_conversation(tenant_id);

-- 对话消息
CREATE TABLE IF NOT EXISTS chat_message (
    message_id       UUID PRIMARY KEY,
    conversation_id  UUID NOT NULL REFERENCES chat_conversation(conversation_id) ON DELETE CASCADE,
    role             VARCHAR(16) NOT NULL,
    content          TEXT NOT NULL,
    chunk_refs       JSONB,
    search_query     TEXT,
    token_count      INT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_chat_msg_conv ON chat_message(conversation_id, created_at);

-- 默认知识库（演示租户）
INSERT INTO vector_library (library_id, tenant_id, name, description, config_json)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'demo',
    '默认知识库',
    '系统预置知识库，兼容历史数据',
    '{
      "metadataDbType": "postgresql",
      "embeddingProvider": "ollama",
      "embeddingModel": "nomic-embed-text",
      "embeddingDimension": 768,
      "chunkingStrategy": "paragraph-first",
      "chunkSize": 600,
      "chunkOverlap": 100,
      "minChunkSize": 80,
      "maxChunkSize": 1200,
      "minParagraphLength": 30,
      "normalizeBeforeChunk": true,
      "textNormalizationEnabled": true
    }'::jsonb
)
ON CONFLICT (library_id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    config_json = EXCLUDED.config_json,
    updated_at = now();
