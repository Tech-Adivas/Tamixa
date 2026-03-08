-- Story retention and completion tracking
-- Privacy: Only parent_id stored, no child PII
CREATE TABLE story_analytics (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id),
    story_id BIGINT NOT NULL,
    story_source VARCHAR(20) NOT NULL DEFAULT 'generated',  -- 'curated' | 'generated' for category joins
    language VARCHAR(10) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    playback_position_seconds INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_story_analytics_parent ON story_analytics(parent_id);
CREATE INDEX idx_story_analytics_story ON story_analytics(story_id);
CREATE INDEX idx_story_analytics_event ON story_analytics(event_type);
CREATE INDEX idx_story_analytics_timestamp ON story_analytics(timestamp);
CREATE INDEX idx_story_analytics_language ON story_analytics(language);
