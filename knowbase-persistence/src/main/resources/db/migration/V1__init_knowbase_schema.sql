CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE kb_library (
    library_id              UUID PRIMARY KEY,
    tenant_id               VARCHAR(64)  NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    description             TEXT,
    status                  VARCHAR(32)  NOT NULL,
    library_type_preset_code VARCHAR(128) NOT NULL,
    tags                    JSONB        NOT NULL DEFAULT '[]'::jsonb,
    created_at              TIMESTAMPTZ  NOT NULL,
    updated_at              TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_kb_library_tenant ON kb_library (tenant_id);

CREATE TABLE kb_library_profile (
    profile_id                      UUID PRIMARY KEY,
    library_id                      UUID         NOT NULL REFERENCES kb_library (library_id),
    version                         INT          NOT NULL,
    embedding_provider              VARCHAR(64)  NOT NULL,
    embedding_model                 VARCHAR(128) NOT NULL,
    embedding_dimension             INT          NOT NULL,
    embedding_tokenizer_profile_id  UUID,
    chunk_max_tokens                INT          NOT NULL,
    chunk_overlap_tokens            INT          NOT NULL,
    retrieval_top_k                 INT          NOT NULL,
    options_json                    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at                      TIMESTAMPTZ  NOT NULL,
    UNIQUE (library_id, version)
);

CREATE TABLE kb_document_profile (
    document_profile_id UUID PRIMARY KEY,
    library_id          UUID         NOT NULL REFERENCES kb_library (library_id),
    code                VARCHAR(128) NOT NULL,
    content_family      VARCHAR(64)  NOT NULL,
    parser_code         VARCHAR(64)  NOT NULL,
    chunking_strategy   VARCHAR(128) NOT NULL,
    tokenizer_profile_id UUID,
    metadata_schema     JSONB        NOT NULL DEFAULT '{}'::jsonb,
    options_json        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (library_id, code)
);

CREATE TABLE kb_index_version (
    index_version_id UUID PRIMARY KEY,
    library_id       UUID        NOT NULL REFERENCES kb_library (library_id),
    profile_id       UUID        NOT NULL,
    version          INT         NOT NULL,
    status           VARCHAR(32) NOT NULL,
    document_count   INT         NOT NULL DEFAULT 0,
    chunk_count      INT         NOT NULL DEFAULT 0,
    published_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL,
    UNIQUE (library_id, version)
);

CREATE INDEX idx_kb_index_version_library_status ON kb_index_version (library_id, status);

CREATE TABLE kb_ingestion_run (
    run_id                   UUID PRIMARY KEY,
    library_id               UUID        NOT NULL REFERENCES kb_library (library_id),
    status                   VARCHAR(32) NOT NULL,
    source_uris              JSONB       NOT NULL,
    source_type              VARCHAR(64),
    document_profile_code    VARCHAR(128),
    publish_index_on_success BOOLEAN     NOT NULL,
    input_documents          INT         NOT NULL,
    succeeded_documents      INT         NOT NULL,
    failed_documents         INT         NOT NULL,
    chunk_count              INT         NOT NULL,
    index_version_id         UUID,
    message                  TEXT,
    options_json             JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at               TIMESTAMPTZ NOT NULL,
    updated_at               TIMESTAMPTZ NOT NULL
);

CREATE TABLE kb_document (
    document_id      UUID PRIMARY KEY,
    library_id       UUID        NOT NULL,
    index_version_id UUID        NOT NULL,
    source_uri       TEXT,
    title            VARCHAR(512),
    created_at       TIMESTAMPTZ NOT NULL
);

CREATE TABLE kb_chunk (
    chunk_id            UUID PRIMARY KEY,
    document_id         UUID         NOT NULL,
    library_id          UUID         NOT NULL,
    index_version_id    UUID         NOT NULL,
    content             TEXT         NOT NULL,
    token_count         INT          NOT NULL,
    tokenizer_id        VARCHAR(128),
    tokenizer_version   VARCHAR(64),
    embedding_model     VARCHAR(128),
    chunk_boundary_type VARCHAR(64),
    parent_chunk_id     UUID,
    metadata_json       JSONB        NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_kb_chunk_index_version ON kb_chunk (index_version_id);

CREATE TABLE kb_embedding (
    embedding_id        UUID PRIMARY KEY,
    chunk_id            UUID         NOT NULL REFERENCES kb_chunk (chunk_id) ON DELETE CASCADE,
    embedding_model     VARCHAR(128) NOT NULL,
    embedding_dimension INT          NOT NULL,
    embedding           vector(1024) NOT NULL
);

CREATE INDEX idx_kb_embedding_chunk ON kb_embedding (chunk_id);
CREATE INDEX idx_kb_embedding_vector ON kb_embedding USING hnsw (embedding vector_cosine_ops);

CREATE TABLE kb_agent (
    agent_id    UUID PRIMARY KEY,
    tenant_id   VARCHAR(64)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_kb_agent_tenant ON kb_agent (tenant_id);

CREATE TABLE kb_agent_version (
    agent_version_id          UUID PRIMARY KEY,
    agent_id                  UUID         NOT NULL REFERENCES kb_agent (agent_id),
    version                   INT          NOT NULL,
    status                    VARCHAR(32)  NOT NULL,
    scene_preset_code         VARCHAR(128) NOT NULL,
    library_ids               JSONB        NOT NULL,
    routing_policy_json       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    retrieval_policy_json     JSONB        NOT NULL DEFAULT '{}'::jsonb,
    answer_policy_json        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    system_prompt             TEXT,
    chat_tokenizer_profile_id UUID,
    published                 BOOLEAN      NOT NULL,
    created_at                TIMESTAMPTZ  NOT NULL,
    UNIQUE (agent_id, version)
);

CREATE TABLE kb_query_run (
    query_run_id      UUID PRIMARY KEY,
    agent_id          UUID        NOT NULL,
    agent_version_id  UUID        NOT NULL,
    status            VARCHAR(32) NOT NULL,
    question          TEXT        NOT NULL,
    answer            TEXT,
    evidence_pack_json JSONB,
    trace_id          VARCHAR(128),
    prompt_tokens     INT         NOT NULL DEFAULT 0,
    completion_tokens INT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL,
    completed_at      TIMESTAMPTZ
);
