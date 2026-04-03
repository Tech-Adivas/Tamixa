-- Parent-facing metadata for curated library stories (discussion prompts, transparency note, speak-along).
ALTER TABLE library_stories
    ADD COLUMN IF NOT EXISTS parent_discussion_prompts JSONB NULL,
    ADD COLUMN IF NOT EXISTS parent_content_note TEXT NULL,
    ADD COLUMN IF NOT EXISTS speak_along_prompt VARCHAR(500) NULL;

COMMENT ON COLUMN library_stories.parent_discussion_prompts IS 'JSON array of short strings: dinner-table prompts for parents after listening.';
COMMENT ON COLUMN library_stories.parent_content_note IS 'Optional note on what was simplified or dramatized for age.';
COMMENT ON COLUMN library_stories.speak_along_prompt IS 'Optional one-line invitation to practice speaking after the story.';
