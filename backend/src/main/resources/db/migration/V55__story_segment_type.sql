-- Segment type for narration vs dialogue (P1: persist emotion/dialogue).
ALTER TABLE story_segments ADD COLUMN IF NOT EXISTS segment_type VARCHAR(32) DEFAULT 'NARRATION';

COMMENT ON COLUMN story_segments.segment_type IS 'NARRATION or DIALOGUE; from EmotionTaggingService';
