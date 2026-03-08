-- Avatar video: SadTalker/Replicate-generated talking-head videos per story+parent+lang+voice.
-- Cached to avoid regenerating; ~75s per video, ~$0.10 per run.

CREATE TABLE IF NOT EXISTS story_avatar_video (
    id BIGSERIAL PRIMARY KEY,
    story_id BIGINT NOT NULL,
    story_source VARCHAR(32) NOT NULL DEFAULT 'curated',
    parent_id BIGINT NOT NULL,
    language VARCHAR(16) NOT NULL DEFAULT 'ta',
    voice_profile VARCHAR(64) NOT NULL DEFAULT 'default',
    storage_path VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    replicate_prediction_id VARCHAR(128),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_story_avatar_video_unique
    ON story_avatar_video(story_id, story_source, parent_id, language, voice_profile);

CREATE INDEX idx_story_avatar_video_status ON story_avatar_video(status);
CREATE INDEX idx_story_avatar_video_parent ON story_avatar_video(parent_id);

ALTER TABLE story_avatar_video ADD CONSTRAINT fk_story_avatar_video_parent
    FOREIGN KEY (parent_id) REFERENCES parents(id) ON DELETE CASCADE;
