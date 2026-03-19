-- Generated story scenes/segments for structured playback (P3: parity with library).
-- Enables precomputed timeline for AI-generated stories; playback uses DB or fallback to runtime split.

CREATE TABLE IF NOT EXISTS generated_story_scenes (
    id BIGSERIAL PRIMARY KEY,
    story_id BIGINT NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
    language VARCHAR(16) NOT NULL DEFAULT 'en',
    scene_index INT NOT NULL DEFAULT 0,
    background_hint VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (story_id, language, scene_index)
);

CREATE INDEX idx_generated_story_scenes_story ON generated_story_scenes(story_id);
CREATE INDEX idx_generated_story_scenes_story_lang ON generated_story_scenes(story_id, language);

CREATE TABLE IF NOT EXISTS generated_story_segments (
    id BIGSERIAL PRIMARY KEY,
    scene_id BIGINT NOT NULL REFERENCES generated_story_scenes(id) ON DELETE CASCADE,
    segment_index INT NOT NULL DEFAULT 0,
    speaker VARCHAR(64) NOT NULL DEFAULT 'Narrator',
    text TEXT NOT NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    audio_url VARCHAR(512),
    segment_type VARCHAR(32) DEFAULT 'NARRATION',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (scene_id, segment_index)
);

CREATE INDEX idx_generated_story_segments_scene ON generated_story_segments(scene_id);

COMMENT ON TABLE generated_story_scenes IS 'Precomputed scenes for AI-generated stories; one row per story/language/scene';
COMMENT ON TABLE generated_story_segments IS 'Precomputed segments for generated story playback; audio_url shared per scene when single TTS';
