-- Enhance story_translations with per-language status tracking for translation, rewrite, content review, audio, and audio review pipelines.
-- This migration supports the Premium UX Overhaul parallel pipeline processing and granular status tracking.
-- Ref: .kiro/specs/tamixa-premium-ux-overhaul/ENHANCEMENTS.md Section 2

-- Translation pipeline tracking (extends existing status column)
ALTER TABLE story_translations
    ADD COLUMN IF NOT EXISTS translation_status VARCHAR(50) DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS translation_started_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS translation_completed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS translation_error TEXT,
    ADD COLUMN IF NOT EXISTS translation_retry_count INT DEFAULT 0;

-- Rewrite pipeline tracking
ALTER TABLE story_translations
    ADD COLUMN IF NOT EXISTS rewrite_status VARCHAR(50) DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS rewrite_started_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rewrite_completed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rewrite_error TEXT,
    ADD COLUMN IF NOT EXISTS rewrite_retry_count INT DEFAULT 0;

-- Content review tracking
ALTER TABLE story_translations
    ADD COLUMN IF NOT EXISTS content_reviewed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS content_reviewed_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS content_review_notes TEXT;

-- Audio pipeline tracking
ALTER TABLE story_translations
    ADD COLUMN IF NOT EXISTS audio_status VARCHAR(50) DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS audio_started_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS audio_completed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS audio_file_url TEXT,
    ADD COLUMN IF NOT EXISTS audio_duration_seconds INT,
    ADD COLUMN IF NOT EXISTS audio_error TEXT,
    ADD COLUMN IF NOT EXISTS audio_retry_count INT DEFAULT 0;

-- Audio review tracking
ALTER TABLE story_translations
    ADD COLUMN IF NOT EXISTS audio_reviewed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS audio_reviewed_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS audio_review_notes TEXT;

-- Add updated_at column for tracking last modification
ALTER TABLE story_translations
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Create indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_story_translations_translation_status ON story_translations(translation_status);
CREATE INDEX IF NOT EXISTS idx_story_translations_rewrite_status ON story_translations(rewrite_status);
CREATE INDEX IF NOT EXISTS idx_story_translations_audio_status ON story_translations(audio_status);
CREATE INDEX IF NOT EXISTS idx_story_translations_content_reviewed ON story_translations(content_reviewed_at) WHERE content_reviewed_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_story_translations_audio_reviewed ON story_translations(audio_reviewed_at) WHERE audio_reviewed_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_story_translations_master_language ON story_translations(master_story_id, language);

-- Add comments for documentation
COMMENT ON COLUMN story_translations.translation_status IS 'Translation pipeline status: PENDING, IN_PROGRESS, COMPLETED, FAILED';
COMMENT ON COLUMN story_translations.rewrite_status IS 'Rewrite pipeline status: PENDING, IN_PROGRESS, COMPLETED, FAILED';
COMMENT ON COLUMN story_translations.audio_status IS 'Audio generation pipeline status: PENDING, IN_PROGRESS, COMPLETED, FAILED';
COMMENT ON COLUMN story_translations.content_reviewed_at IS 'Timestamp when content was reviewed by human reviewer';
COMMENT ON COLUMN story_translations.audio_reviewed_at IS 'Timestamp when audio was reviewed and approved by human reviewer';

