-- Per-language approval for narrated content (human verification).
ALTER TABLE story_translations
    ADD COLUMN IF NOT EXISTS narration_approved_at TIMESTAMP;
