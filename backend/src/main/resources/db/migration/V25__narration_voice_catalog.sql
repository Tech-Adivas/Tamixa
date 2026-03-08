-- TTS voice catalog for premium monetization.
-- Maps voice identifiers (default, calm, excited) to isPremium.
-- Distinct from voice_profiles (cloned parent voice).

CREATE TABLE IF NOT EXISTS narration_voice_catalog (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(64) NOT NULL,
    language VARCHAR(16) NOT NULL,
    voice_name VARCHAR(128) NOT NULL,
    tone_mode VARCHAR(64) NOT NULL,
    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE narration_voice_catalog ADD CONSTRAINT uk_narration_voice_catalog_provider_tone UNIQUE (provider, language, tone_mode);
CREATE INDEX idx_narration_voice_catalog_tone ON narration_voice_catalog(tone_mode);
CREATE INDEX idx_narration_voice_catalog_active ON narration_voice_catalog(is_active) WHERE is_active = TRUE;

-- Seed default (free) and calm (premium)
INSERT INTO narration_voice_catalog (provider, language, voice_name, tone_mode, is_premium, is_active)
VALUES ('neural', 'en', 'default', 'default', FALSE, TRUE), ('neural', 'en', 'calm', 'calm', TRUE, TRUE)
ON CONFLICT (provider, language, tone_mode) DO NOTHING;
