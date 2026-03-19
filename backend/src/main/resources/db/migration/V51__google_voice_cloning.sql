-- Google Cloud Chirp 3 Instant Custom Voice for voice cloning (replaces ElevenLabs).
-- Voice cloning key is stored per profile for TTS synthesis.
ALTER TABLE voice_profiles ADD COLUMN IF NOT EXISTS google_voice_cloning_key TEXT;
CREATE INDEX IF NOT EXISTS idx_voice_profiles_google_key ON voice_profiles(parent_id) WHERE google_voice_cloning_key IS NOT NULL;

-- Consent audio path for Google voice cloning (parent must record consent statement).
ALTER TABLE voice_cloning_jobs ADD COLUMN IF NOT EXISTS consent_audio_storage_path VARCHAR(500);
ALTER TABLE voice_cloning_jobs ADD COLUMN IF NOT EXISTS google_voice_cloning_key TEXT;
