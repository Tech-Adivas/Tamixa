-- Rename curated_stories to library_stories; align story_source 'curated' -> 'library'
ALTER TABLE curated_stories RENAME TO library_stories;

ALTER INDEX IF EXISTS idx_curated_stories_language RENAME TO idx_library_stories_language;
ALTER INDEX IF EXISTS idx_curated_stories_age RENAME TO idx_library_stories_age;
ALTER INDEX IF EXISTS idx_curated_stories_theme RENAME TO idx_library_stories_theme;
ALTER INDEX IF EXISTS idx_curated_stories_created_at RENAME TO idx_library_stories_created_at;
ALTER INDEX IF EXISTS idx_curated_stories_status RENAME TO idx_library_stories_status;
ALTER INDEX IF EXISTS idx_curated_stories_emotion_mode RENAME TO idx_library_stories_emotion_mode;

-- Update story_source: 'curated' -> 'library' in all tables
UPDATE story_analytics SET story_source = 'library' WHERE story_source = 'curated';
UPDATE favorite_story SET story_source = 'library' WHERE story_source = 'curated';
UPDATE story_feedback SET story_source = 'library' WHERE story_source = 'curated';
UPDATE story_playback_position SET story_source = 'library' WHERE story_source = 'curated';
UPDATE story_avatar_video SET story_source = 'library' WHERE story_source = 'curated';
UPDATE story_voice_preference SET story_source = 'library' WHERE story_source = 'curated';

-- Update defaults for tables that default to 'curated'
ALTER TABLE story_voice_preference ALTER COLUMN story_source SET DEFAULT 'library';
ALTER TABLE story_avatar_video ALTER COLUMN story_source SET DEFAULT 'library';
