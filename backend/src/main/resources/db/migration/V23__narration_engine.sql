-- Conversational Narration Engine: AI-enhanced scripts and neural TTS audio.
-- story_narration_scripts: AI-formatted scripts per translation (tone, safety).
-- story_narration_audio: neural TTS output per translation (voice, duration, status).

CREATE TABLE IF NOT EXISTS story_narration_scripts (
    id BIGSERIAL PRIMARY KEY,
    translation_id BIGINT NOT NULL REFERENCES story_translations(id) ON DELETE CASCADE,
    tone_mode VARCHAR(20) NOT NULL DEFAULT 'CALM',
    script_text TEXT NOT NULL,
    safety_score INT NOT NULL DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (translation_id)
);

CREATE INDEX idx_narration_scripts_translation ON story_narration_scripts(translation_id);

CREATE TABLE IF NOT EXISTS story_narration_audio (
    id BIGSERIAL PRIMARY KEY,
    translation_id BIGINT NOT NULL REFERENCES story_translations(id) ON DELETE CASCADE,
    voice_profile VARCHAR(100) NOT NULL DEFAULT 'default',
    audio_url VARCHAR(512) NOT NULL,
    duration_seconds INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (translation_id)
);

CREATE INDEX idx_narration_audio_translation ON story_narration_audio(translation_id);
CREATE INDEX idx_narration_audio_status ON story_narration_audio(status);
