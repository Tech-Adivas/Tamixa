-- Persisted scenes and segments for playback manifest.
-- Enables precomputed timeline; playback can use DB or fall back to runtime split.
CREATE TABLE IF NOT EXISTS story_scenes (
    id BIGSERIAL PRIMARY KEY,
    translation_id BIGINT NOT NULL REFERENCES story_translations(id) ON DELETE CASCADE,
    scene_index INT NOT NULL DEFAULT 0,
    background_hint VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (translation_id, scene_index)
);

CREATE INDEX idx_story_scenes_translation ON story_scenes(translation_id);

CREATE TABLE IF NOT EXISTS story_segments (
    id BIGSERIAL PRIMARY KEY,
    scene_id BIGINT NOT NULL REFERENCES story_scenes(id) ON DELETE CASCADE,
    segment_index INT NOT NULL DEFAULT 0,
    speaker VARCHAR(64) NOT NULL DEFAULT 'Narrator',
    text TEXT NOT NULL,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    audio_url VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (scene_id, segment_index)
);

CREATE INDEX idx_story_segments_scene ON story_segments(scene_id);

COMMENT ON TABLE story_scenes IS 'Precomputed scenes per translation; MVP: one scene per language';
COMMENT ON TABLE story_segments IS 'Precomputed segments for subtitle sync; audio_url nullable (shared per scene when single TTS)';
