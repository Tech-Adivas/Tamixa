ALTER TABLE voice_profiles
    ADD COLUMN IF NOT EXISTS reference_audio_path VARCHAR(512);

