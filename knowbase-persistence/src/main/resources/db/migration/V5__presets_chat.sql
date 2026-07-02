CREATE TABLE kb_library_type_preset (
    preset_id   UUID PRIMARY KEY,
    tenant_id   VARCHAR(64),
    code        VARCHAR(128) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    config_json JSONB        NOT NULL DEFAULT '{}'::jsonb,
    built_in    BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL,
  UNIQUE (tenant_id, code)
);

CREATE TABLE kb_scene_rule_preset (
    preset_id   UUID PRIMARY KEY,
    tenant_id   VARCHAR(64),
    code        VARCHAR(128) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    config_json JSONB        NOT NULL DEFAULT '{}'::jsonb,
    built_in    BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL,
    UNIQUE (tenant_id, code)
);

CREATE TABLE kb_chat_session (
    session_id        UUID PRIMARY KEY,
    tenant_id         VARCHAR(64)  NOT NULL,
    agent_id          UUID         NOT NULL,
    agent_version_id  UUID,
    title             VARCHAR(255),
    status            VARCHAR(32)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_kb_chat_session_tenant ON kb_chat_session (tenant_id);

CREATE TABLE kb_chat_message (
    message_id   UUID PRIMARY KEY,
    session_id   UUID        NOT NULL REFERENCES kb_chat_session (session_id),
    role         VARCHAR(32) NOT NULL,
    content      TEXT        NOT NULL,
    query_run_id UUID,
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_kb_chat_message_session ON kb_chat_message (session_id);
