-- ROLLBACK SCRIPT for V99__unified_library_story_status.sql
-- This is a reference script for manual rollback if needed.
-- Flyway does not execute this automatically; it must be run manually if rollback is required.

-- WARNING: This rollback script should only be used if V99 migration needs to be reverted.
-- It restores the old status column and drops the new unified status system.

-- Step 1: Add back the old status column (temporarily)
ALTER TABLE library_stories ADD COLUMN IF NOT EXISTS old_status VARCHAR(20);

-- Step 2: Map unified status back to legacy status values
UPDATE library_stories
SET old_status = CASE
    WHEN status = 'DRAFT' THEN 'DRAFT'
    WHEN status = 'SUBMITTED' THEN 'PUBLISHED'
    WHEN status = 'TRANSLATING' THEN 'PROCESSING'
    WHEN status = 'TRANSLATION_FAILED' THEN 'PROCESSING'
    WHEN status = 'CONTENT_REVIEW' THEN 'PUBLISHED'
    WHEN status = 'CHANGES_REQUESTED' THEN 'CHANGES_REQUESTED'
    WHEN status = 'REJECTED' THEN 'REJECTED'
    WHEN status = 'APPROVED' THEN 'READY'
    WHEN status = 'AUDIO_GENERATING' THEN 'PROCESSING'
    WHEN status = 'AUDIO_FAILED' THEN 'PROCESSING'
    WHEN status = 'AUDIO_REVIEW' THEN 'READY'
    WHEN status = 'PUBLISHED' THEN 'PUBLISHED'
    ELSE 'DRAFT'
END;

-- Step 3: Drop the unified status column
ALTER TABLE library_stories DROP COLUMN status;

-- Step 4: Drop legacy_status column (if it exists from V99)
ALTER TABLE library_stories DROP COLUMN IF EXISTS legacy_status;

-- Step 5: Rename old_status back to status
ALTER TABLE library_stories RENAME COLUMN old_status TO status;

-- Step 6: Restore original column constraints
ALTER TABLE library_stories ALTER COLUMN status SET NOT NULL;
ALTER TABLE library_stories ALTER COLUMN status SET DEFAULT 'DRAFT';
ALTER TABLE library_stories ALTER COLUMN status TYPE VARCHAR(20);

-- Step 7: Drop new indexes
DROP INDEX IF EXISTS idx_library_stories_status_narration;
DROP INDEX IF EXISTS idx_library_stories_status_created;

-- Step 8: Recreate original index
CREATE INDEX IF NOT EXISTS idx_library_stories_status ON library_stories(status);

-- Step 9: Remove comments
COMMENT ON COLUMN library_stories.status IS NULL;

