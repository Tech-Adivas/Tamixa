-- Premium avatar: parent uploads image; used as storyteller in generated videos.
-- One avatar per parent.

CREATE TABLE IF NOT EXISTS parent_avatar (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL UNIQUE,
    storage_path VARCHAR(512) NOT NULL,
    content_type VARCHAR(64) NOT NULL DEFAULT 'image/jpeg',
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_parent_avatar_parent ON parent_avatar(parent_id);

ALTER TABLE parent_avatar ADD CONSTRAINT fk_parent_avatar_parent
    FOREIGN KEY (parent_id) REFERENCES parents(id) ON DELETE CASCADE;
