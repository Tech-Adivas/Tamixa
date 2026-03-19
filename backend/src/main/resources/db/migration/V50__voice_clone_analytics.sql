-- Voice clone usage analytics: creation and narration usage
-- Enables product metrics and entitlement checks for voice cloning
CREATE TABLE voice_clone_analytics (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    event_type VARCHAR(30) NOT NULL,
    voice_profile_id BIGINT,
    voice_cloning_job_id BIGINT,
    story_id BIGINT,
    story_source VARCHAR(20),
    language VARCHAR(10),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_voice_clone_analytics_parent ON voice_clone_analytics(parent_id);
CREATE INDEX idx_voice_clone_analytics_event ON voice_clone_analytics(event_type);
CREATE INDEX idx_voice_clone_analytics_created ON voice_clone_analytics(created_at);

COMMENT ON TABLE voice_clone_analytics IS 'Tracks voice clone creation (VOICE_CLONE_CREATED) and usage in narration (VOICE_CLONE_USED)';
