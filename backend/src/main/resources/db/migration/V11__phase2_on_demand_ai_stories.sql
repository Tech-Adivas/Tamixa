-- Phase 2: On-demand AI stories – personalization, emotion modes, voice cloning, parent prompts

-- Voice profiles for voice cloning (required before stories.voice_profile_id FK)
CREATE TABLE IF NOT EXISTS voice_profiles (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    encrypted_embedding BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_voice_profiles_parent_id ON voice_profiles(parent_id);

-- Child preferences for personalization tokens
ALTER TABLE children ADD COLUMN interests VARCHAR(500);

-- Story generation context (per-child, emotion, voice, custom prompt)
ALTER TABLE stories ADD COLUMN voice_profile_id BIGINT REFERENCES voice_profiles(id);
ALTER TABLE stories ADD COLUMN emotion_mode VARCHAR(20);
ALTER TABLE stories ADD COLUMN parent_custom_prompt TEXT;

CREATE INDEX idx_stories_voice_profile ON stories(voice_profile_id);
CREATE INDEX idx_stories_emotion_mode ON stories(emotion_mode);
