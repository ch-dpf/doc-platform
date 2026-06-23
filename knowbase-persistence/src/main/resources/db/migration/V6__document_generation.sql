ALTER TABLE kb_library
    ADD COLUMN active_index_generation_id UUID REFERENCES kb_index_version (index_version_id);

ALTER TABLE kb_document
    ADD COLUMN status              VARCHAR(32)  NOT NULL DEFAULT 'INDEXED',
    ADD COLUMN document_profile_id UUID,
    ADD COLUMN content_hash        VARCHAR(128),
    ADD COLUMN last_indexed_at     TIMESTAMPTZ,
    ADD COLUMN last_error          TEXT,
    ADD COLUMN updated_at          TIMESTAMPTZ;

UPDATE kb_document SET updated_at = created_at WHERE updated_at IS NULL;

ALTER TABLE kb_document ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE kb_document ALTER COLUMN updated_at SET DEFAULT NOW();

CREATE INDEX idx_kb_document_library_source ON kb_document (library_id, source_uri);
CREATE INDEX idx_kb_document_library_status ON kb_document (library_id, status);

-- Backfill active generation from latest published index version per library.
UPDATE kb_library lib
SET active_index_generation_id = sub.index_version_id
FROM (
    SELECT DISTINCT ON (library_id)
           library_id,
           index_version_id
    FROM kb_index_version
    WHERE status = 'PUBLISHED'
    ORDER BY library_id, version DESC
) sub
WHERE lib.library_id = sub.library_id;

UPDATE kb_document
SET last_indexed_at = created_at,
    updated_at = created_at
WHERE last_indexed_at IS NULL;
