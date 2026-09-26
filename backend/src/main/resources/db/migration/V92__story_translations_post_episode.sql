-- Per-locale post-episode family mission and optional resource link (parent-facing).
-- Null = inherit from library_stories for that story.

ALTER TABLE story_translations
    ADD COLUMN IF NOT EXISTS post_story_mission TEXT,
    ADD COLUMN IF NOT EXISTS post_story_resource_url VARCHAR(512);
