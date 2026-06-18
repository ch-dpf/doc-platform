CREATE TABLE kb_tokenizer_profile (
    tokenizer_profile_id UUID PRIMARY KEY,
    provider              VARCHAR(64)  NOT NULL,
    model_name            VARCHAR(128) NOT NULL,
    tokenizer_id          VARCHAR(128) NOT NULL,
    tokenizer_version     VARCHAR(64)  NOT NULL,
    approximate           BOOLEAN      NOT NULL DEFAULT TRUE,
    config_json           JSONB        NOT NULL DEFAULT '{}'::jsonb,
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (provider, model_name)
);

CREATE INDEX idx_kb_tokenizer_profile_provider ON kb_tokenizer_profile (provider, enabled);
