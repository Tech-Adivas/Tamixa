-- Soft-delete: stories stay recoverable until scheduled purge (default 30 days, app.library-story-soft-delete.retention-days).
ALTER TABLE library_stories ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_library_stories_deleted_at ON library_stories(deleted_at)
    WHERE deleted_at IS NOT NULL;

COMMENT ON COLUMN library_stories.deleted_at IS 'When set, story is hidden from app/admin lists; row removed permanently after retention period.';
