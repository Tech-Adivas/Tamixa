-- Optional HeyGen voice_id for TTS (create voice in HeyGen app, then set here for narration)
ALTER TABLE voice_profiles ADD COLUMN IF NOT EXISTS heygen_voice_id VARCHAR(128);
CREATE INDEX IF NOT EXISTS idx_voice_profiles_heygen_voice_id ON voice_profiles(heygen_voice_id) WHERE heygen_voice_id IS NOT NULL;
