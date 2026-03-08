-- Multi-voice support: allow multiple audio files per translation (one per voice_profile).
-- Idempotency: (translation_id, voice_profile) uniquely identifies a narration audio.
-- Removes single-translation constraint; enables per-voice generation without duplicates.

ALTER TABLE story_narration_audio DROP CONSTRAINT IF EXISTS story_narration_audio_translation_id_key;

-- Composite unique: one audio row per (translation_id, voice_profile)
ALTER TABLE story_narration_audio
ADD CONSTRAINT uk_narration_audio_translation_voice UNIQUE (translation_id, voice_profile);

CREATE INDEX IF NOT EXISTS idx_narration_audio_translation_voice
ON story_narration_audio(translation_id, voice_profile);
