-- Per-story voice preference: parent can choose to use their cloned voice for selected stories.
-- When they open the story, the app pre-selects this voice.
CREATE TABLE story_voice_preference (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    story_id BIGINT NOT NULL,
    story_source VARCHAR(20) NOT NULL DEFAULT 'curated',
    voice_profile VARCHAR(100) NOT NULL DEFAULT 'default',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(parent_id, story_id, story_source)
);

CREATE INDEX idx_story_voice_preference_parent ON story_voice_preference(parent_id);
CREATE INDEX idx_story_voice_preference_story ON story_voice_preference(story_id);
