-- Fish Audio managed voice clone (alternative to ElevenLabs): model id from POST /model, TTS via POST /v1/tts
ALTER TABLE voice_profiles ADD COLUMN IF NOT EXISTS fish_audio_model_id VARCHAR(128);
CREATE INDEX IF NOT EXISTS idx_voice_profiles_fish_audio_model_id ON voice_profiles(fish_audio_model_id) WHERE fish_audio_model_id IS NOT NULL;

ALTER TABLE voice_cloning_jobs ADD COLUMN IF NOT EXISTS fish_audio_model_id VARCHAR(128);
