-- Playback mode per story: default (system voice), my_voice (cloned), avatar (talking-head video).
-- Enables restoring "Avatar" vs "My Voice" selection when reopening the story.
ALTER TABLE story_voice_preference
    ADD COLUMN IF NOT EXISTS playback_mode VARCHAR(20) NOT NULL DEFAULT 'default';

COMMENT ON COLUMN story_voice_preference.playback_mode IS 'default | my_voice | avatar';
