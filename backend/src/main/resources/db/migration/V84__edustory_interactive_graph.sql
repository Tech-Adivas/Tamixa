-- EduStory pilot: branching narrative graph (JSON), post-episode mission + optional resource link, life-skill choice audit trail.
ALTER TABLE library_stories ADD COLUMN IF NOT EXISTS interactive_graph TEXT;
ALTER TABLE library_stories ADD COLUMN IF NOT EXISTS post_story_mission TEXT;
ALTER TABLE library_stories ADD COLUMN IF NOT EXISTS post_story_resource_url VARCHAR(512);

CREATE TABLE IF NOT EXISTS life_skill_choice_events (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    child_id BIGINT NOT NULL REFERENCES children(id) ON DELETE CASCADE,
    library_story_id BIGINT NOT NULL REFERENCES library_stories(id) ON DELETE CASCADE,
    segment_id VARCHAR(128) NOT NULL,
    choice_id VARCHAR(128) NOT NULL,
    skill_deltas JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_life_skill_choice_parent_created ON life_skill_choice_events(parent_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_life_skill_choice_child ON life_skill_choice_events(child_id);
