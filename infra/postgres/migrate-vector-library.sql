-- 自旧版（无 vector_library）升级：先备份数据，再在空库或测试环境执行
-- 全新安装请直接使用 init.sql

CREATE EXTENSION IF NOT EXISTS vector;
SET search_path TO public;

-- 若表已存在则跳过创建（手工升级时按需执行各段）

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

DROP TABLE IF EXISTS ingest_orchestration CASCADE;

CREATE TABLE IF NOT EXISTS upload_task (
    task_id          UUID PRIMARY KEY,
    library_id       UUID NOT NULL,
    tenant_id        VARCHAR(64) NOT NULL,
    file_name        VARCHAR(512) NOT NULL,
    status           VARCHAR(32) NOT NULL,
    progress         INT NOT NULL DEFAULT 0,
    doc_id           UUID,
    error_message    TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 默认库
INSERT INTO vector_library (library_id, tenant_id, name, description, config_json)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'demo',
    '默认知识库',
    '迁移生成的默认知识库',
    '{"storageType":"minio","metadataDbType":"postgresql","embeddingProvider":"ollama","embeddingDimension":768,"chunkingStrategy":"paragraph-first","chunkSize":600,"chunkOverlap":100}'::jsonb
)
ON CONFLICT (library_id) DO NOTHING;

ALTER TABLE doc_metadata ADD COLUMN IF NOT EXISTS library_id UUID;
UPDATE doc_metadata SET library_id = '00000000-0000-0000-0000-000000000001' WHERE library_id IS NULL;

ALTER TABLE document_chunk ADD COLUMN IF NOT EXISTS library_id UUID;
UPDATE document_chunk SET library_id = '00000000-0000-0000-0000-000000000001' WHERE library_id IS NULL;

ALTER TABLE document_index_job ADD COLUMN IF NOT EXISTS library_id UUID;
UPDATE document_index_job SET library_id = '00000000-0000-0000-0000-000000000001' WHERE library_id IS NULL;
