-- Production narration pipeline: Extend STORY_TRANSLATION with status state machine.
-- States: PENDING, TRANSLATING, TRANSLATION_FAILED, REWRITING, REWRITE_FAILED, TTS_PROCESSING, TTS_FAILED, COMPLETED
-- Atomic transitions for retry and failure recovery.

ALTER TABLE story_translations
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_error TEXT;

CREATE INDEX IF NOT EXISTS idx_story_translations_status ON story_translations(status);
CREATE INDEX IF NOT EXISTS idx_story_translations_status_updated ON story_translations(status, created_at);

COMMENT ON COLUMN story_translations.status IS 'Pipeline state: PENDING, TRANSLATING, TRANSLATION_FAILED, REWRITING, REWRITE_FAILED, TTS_PROCESSING, TTS_FAILED, COMPLETED';
