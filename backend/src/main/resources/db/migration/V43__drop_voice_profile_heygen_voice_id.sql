-- Revert HeyGen TTS from voice profiles; voice cloning uses ElevenLabs/XTTS only
ALTER TABLE voice_profiles DROP COLUMN IF EXISTS heygen_voice_id;
