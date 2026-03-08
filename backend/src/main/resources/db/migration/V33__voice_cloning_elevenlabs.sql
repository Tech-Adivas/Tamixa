-- Voice cloning: store ElevenLabs voice_id for TTS synthesis
-- When parent uploads audio sample, we create voice via ElevenLabs API and store the returned voice_id
ALTER TABLE voice_profiles ADD COLUMN IF NOT EXISTS elevenlabs_voice_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_voice_profiles_elevenlabs_id ON voice_profiles(elevenlabs_voice_id) WHERE elevenlabs_voice_id IS NOT NULL;
