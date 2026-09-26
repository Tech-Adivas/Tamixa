-- Rollback script for V100__enhance_story_translations_pipeline_tracking.sql
-- This script removes the enhanced pipeline tracking columns added in V100.
-- WARNING: This will result in data loss for pipeline tracking information.

-- Drop indexes first
DROP INDEX IF EXISTS idx_story_translations_translation_status;
DROP INDEX IF EXISTS idx_story_translations_rewrite_status;
DROP INDEX IF EXISTS idx_story_translations_audio_status;
DROP INDEX IF EXISTS idx_story_translations_content_reviewed;
DROP INDEX IF EXISTS idx_story_translations_audio_reviewed;
DROP INDEX IF EXISTS idx_story_translations_master_language;

-- Drop translation pipeline columns
ALTER TABLE story_translations
    DROP COLUMN IF EXISTS translation_status,
    DROP COLUMN IF EXISTS translation_started_at,
    DROP COLUMN IF EXISTS translation_completed_at,
    DROP COLUMN IF EXISTS translation_error,
    DROP COLUMN IF EXISTS translation_retry_count;

-- Drop rewrite pipeline columns
ALTER TABLE story_translations
    DROP COLUMN IF EXISTS rewrite_status,
    DROP COLUMN IF EXISTS rewrite_started_at,
    DROP COLUMN IF EXISTS rewrite_completed_at,
    DROP COLUMN IF EXISTS rewrite_error,
    DROP COLUMN IF EXISTS rewrite_retry_count;

-- Drop content review columns
ALTER TABLE story_translations
    DROP COLUMN IF EXISTS content_reviewed_at,
    DROP COLUMN IF EXISTS content_reviewed_by,
    DROP COLUMN IF EXISTS content_review_notes;

-- Drop audio pipeline columns
ALTER TABLE story_translations
    DROP COLUMN IF EXISTS audio_status,
    DROP COLUMN IF EXISTS audio_started_at,
    DROP COLUMN IF EXISTS audio_completed_at,
    DROP COLUMN IF EXISTS audio_file_url,
    DROP COLUMN IF EXISTS audio_duration_seconds,
    DROP COLUMN IF EXISTS audio_error,
    DROP COLUMN IF EXISTS audio_retry_count;

-- Drop audio review columns
ALTER TABLE story_translations
    DROP COLUMN IF EXISTS audio_reviewed_at,
    DROP COLUMN IF EXISTS audio_reviewed_by,
    DROP COLUMN IF EXISTS audio_review_notes;

-- Drop updated_at column
ALTER TABLE story_translations
    DROP COLUMN IF EXISTS updated_at;

