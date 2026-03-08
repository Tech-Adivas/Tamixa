-- Add emotion_mode to curated_stories for narration tone consistency.
-- Maps to ToneMode: CALM/SOOTHING -> CALM, ADVENTUROUS -> EXPRESSIVE.
ALTER TABLE curated_stories ADD COLUMN IF NOT EXISTS emotion_mode VARCHAR(20) DEFAULT 'CALM';
CREATE INDEX IF NOT EXISTS idx_curated_stories_emotion_mode ON curated_stories(emotion_mode);
