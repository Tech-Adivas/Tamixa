-- Family recorded voice: parent uploads MP3 per story. Stored in private path.
-- One recording per (story_id, parent_id, language).

CREATE TABLE IF NOT EXISTS story_family_voice (
    id BIGSERIAL PRIMARY KEY,
    story_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL,
    language VARCHAR(16) NOT NULL DEFAULT 'ta',
    storage_path VARCHAR(512) NOT NULL,
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_story_family_voice_parent_lang UNIQUE (story_id, parent_id, language)
);

CREATE INDEX idx_story_family_voice_story ON story_family_voice(story_id);
CREATE INDEX idx_story_family_voice_parent ON story_family_voice(parent_id);

ALTER TABLE story_family_voice ADD CONSTRAINT fk_story_family_voice_parent
    FOREIGN KEY (parent_id) REFERENCES parents(id) ON DELETE CASCADE;
