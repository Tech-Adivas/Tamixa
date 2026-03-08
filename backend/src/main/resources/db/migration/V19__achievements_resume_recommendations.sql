-- Optional child_id for analytics (enables per-child achievements)
ALTER TABLE story_analytics ADD COLUMN IF NOT EXISTS child_id BIGINT REFERENCES children(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_story_analytics_child ON story_analytics(child_id);

-- Resume playback: last position per story (parent-scoped; optional child for multi-child)
CREATE TABLE IF NOT EXISTS story_playback_position (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    story_id BIGINT NOT NULL,
    story_source VARCHAR(20) NOT NULL DEFAULT 'generated',
    position_seconds INT NOT NULL DEFAULT 0,
    child_id BIGINT REFERENCES children(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(parent_id, story_id, story_source)
);

CREATE INDEX idx_playback_position_parent ON story_playback_position(parent_id);
CREATE INDEX idx_playback_position_updated ON story_playback_position(updated_at DESC);
