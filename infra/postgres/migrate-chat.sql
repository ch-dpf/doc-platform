-- 为已有 knowbase 库增加对话持久化表
SET client_encoding TO 'UTF8';

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
