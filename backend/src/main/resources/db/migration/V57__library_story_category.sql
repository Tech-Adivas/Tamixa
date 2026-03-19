-- Add category column to library_stories (distinct from theme for browse/filter).
-- Theme = story setting (e.g. forest, village); category = content type (e.g. adventure, moral).
ALTER TABLE library_stories ADD COLUMN IF NOT EXISTS category VARCHAR(100);

-- Backfill: use theme as category for existing rows
UPDATE library_stories SET category = theme WHERE category IS NULL AND theme IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_library_stories_category ON library_stories(category);

COMMENT ON COLUMN library_stories.category IS 'Content category for browse/filter; distinct from theme (setting)';
