-- Share clips: Premium+ users can create 15-60s video clips for Instagram/YouTube.
-- Format: 9:16 (Reels/TikTok), 1:1 (IG feed). Async job extracts segment, composites avatar+audio, adds watermark.
-- Quota enforced in service layer (e.g. 5-10 clips per parent per month).

CREATE TABLE IF NOT EXISTS shareable_clips (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    story_id BIGINT NOT NULL,
    story_source VARCHAR(32) NOT NULL DEFAULT 'library',
    language VARCHAR(16) NOT NULL DEFAULT 'ta',
    voice_profile VARCHAR(64) NOT NULL DEFAULT 'default',
    start_seconds INT NOT NULL DEFAULT 0,
    duration_seconds INT NOT NULL DEFAULT 30,
    format VARCHAR(10) NOT NULL DEFAULT '9:16',
    storage_path VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    processing_job_id BIGINT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shareable_clips_parent_created ON shareable_clips(parent_id, created_at);
CREATE INDEX idx_shareable_clips_status ON shareable_clips(status);
CREATE INDEX idx_shareable_clips_story ON shareable_clips(story_id, story_source);

COMMENT ON TABLE shareable_clips IS 'Premium+ share clip generation: avatar+audio segment, watermark, ready for Instagram/YouTube';
