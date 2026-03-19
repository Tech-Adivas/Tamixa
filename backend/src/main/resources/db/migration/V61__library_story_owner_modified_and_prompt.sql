-- Track creator ownership, modification time, and conversion prompt used for story pipeline submissions.
ALTER TABLE library_stories
    ADD COLUMN IF NOT EXISTS story_owner VARCHAR(255);

ALTER TABLE library_stories
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE library_stories
    ADD COLUMN IF NOT EXISTS convert_prompt_used TEXT;

-- Backfill owner for existing records.
UPDATE library_stories
SET story_owner = COALESCE(NULLIF(TRIM(story_owner), ''), 'system')
WHERE story_owner IS NULL OR TRIM(story_owner) = '';

CREATE INDEX IF NOT EXISTS idx_library_stories_updated_at ON library_stories(updated_at);
