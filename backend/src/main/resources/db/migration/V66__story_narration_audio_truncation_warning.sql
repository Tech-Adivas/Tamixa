-- Store audio coverage warning per narration row (audio may be truncated vs expected).
-- Set when duration < 50% of expected; pipeline status reads from here instead of computing.

ALTER TABLE story_narration_audio
ADD COLUMN IF NOT EXISTS truncation_warning BOOLEAN NOT NULL DEFAULT false;
