-- Unified LibraryStoryStatus system: Replace confusing separation between story status and pipeline status.
-- New workflow: DRAFT → SUBMITTED → TRANSLATING → CONTENT_REVIEW → APPROVED → AUDIO_GENERATING → AUDIO_REVIEW → PUBLISHED
-- Preserves existing data with backward-compatible mapping.

-- Step 1: Add new status column (nullable initially for safe migration)
ALTER TABLE library_stories ADD COLUMN IF NOT EXISTS unified_status VARCHAR(30);

-- Step 2: Migrate existing status values to new unified system
-- Mapping logic:
--   DRAFT → DRAFT (no change)
--   PUBLISHED → CONTENT_REVIEW (old "PUBLISHED" meant "in review queue", not live)
--   PROCESSING → TRANSLATING (pipeline running)
--   READY → APPROVED (content approved, ready for audio)
--   CHANGES_REQUESTED → CHANGES_REQUESTED (no change)
--   REJECTED → REJECTED (no change)

UPDATE library_stories
SET unified_status = CASE
    WHEN status = 'DRAFT' THEN 'DRAFT'
    WHEN status = 'PUBLISHED' THEN 'CONTENT_REVIEW'
    WHEN status = 'PROCESSING' THEN 'TRANSLATING'
    WHEN status = 'READY' THEN 'APPROVED'
    WHEN status = 'CHANGES_REQUESTED' THEN 'CHANGES_REQUESTED'
    WHEN status = 'REJECTED' THEN 'REJECTED'
    ELSE 'DRAFT'  -- Fallback for any unexpected values
END
WHERE unified_status IS NULL;

-- Step 3: Make unified_status NOT NULL with default
ALTER TABLE library_stories ALTER COLUMN unified_status SET NOT NULL;
ALTER TABLE library_stories ALTER COLUMN unified_status SET DEFAULT 'DRAFT';

-- Step 4: Rename old status column to legacy_status for backward compatibility
ALTER TABLE library_stories RENAME COLUMN status TO legacy_status;

-- Step 5: Rename unified_status to status
ALTER TABLE library_stories RENAME COLUMN unified_status TO status;

-- Step 6: Update status column length to accommodate new values
ALTER TABLE library_stories ALTER COLUMN status TYPE VARCHAR(30);

-- Step 7: Add index for new status values (drop old index first if exists)
DROP INDEX IF EXISTS idx_library_stories_status;
CREATE INDEX idx_library_stories_status ON library_stories(status) WHERE deleted_at IS NULL;

-- Step 8: Add composite index for common queries (status + narration approval)
CREATE INDEX IF NOT EXISTS idx_library_stories_status_narration 
    ON library_stories(status, narration_approved_at) 
    WHERE deleted_at IS NULL;

-- Step 9: Add index for workflow queries (status + created_at for ordering)
CREATE INDEX IF NOT EXISTS idx_library_stories_status_created 
    ON library_stories(status, created_at DESC) 
    WHERE deleted_at IS NULL;

-- Step 10: Add comments for documentation
COMMENT ON COLUMN library_stories.status IS 
    'Unified workflow status: DRAFT, SUBMITTED, TRANSLATING, TRANSLATION_FAILED, CONTENT_REVIEW, CHANGES_REQUESTED, REJECTED, APPROVED, AUDIO_GENERATING, AUDIO_FAILED, AUDIO_REVIEW, PUBLISHED';

COMMENT ON COLUMN library_stories.legacy_status IS 
    'Preserved old status values for backward compatibility and audit trail. Do not use for new logic.';

-- Step 11: Add migration metadata for rollback reference
COMMENT ON INDEX idx_library_stories_status IS 
    'V99: Unified status system - supports new workflow phases (translation, content review, audio generation, audio review)';

