-- Optional per-scene storybook art (Phase 2 narrative visuals). Paths resolved like cover_image_url.
ALTER TABLE story_scenes ADD COLUMN IF NOT EXISTS illustration_image_path VARCHAR(512);
ALTER TABLE generated_story_scenes ADD COLUMN IF NOT EXISTS illustration_image_path VARCHAR(512);
