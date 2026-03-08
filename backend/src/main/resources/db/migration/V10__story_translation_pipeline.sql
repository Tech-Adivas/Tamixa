-- Translation and TTS pipeline for curated stories.
-- MASTER_STORIES = curated_stories (Tamil source)
-- story_translations: per-language translated content (unique per master_story_id + language)
-- story_audio: per-language TTS output (unique per master_story_id + language)
-- story_processing_status: job tracking with idempotency and retry support

-- Translated story content per language (excludes Tamil source in curated_stories)
CREATE TABLE IF NOT EXISTS story_translations (
    id BIGSERIAL PRIMARY KEY,
    master_story_id BIGINT NOT NULL REFERENCES curated_stories(id) ON DELETE CASCADE,
    language VARCHAR(10) NOT NULL,
    title VARCHAR(255),
    content TEXT NOT NULL,
    moral TEXT,
    word_count INT NOT NULL DEFAULT 0,
    reading_time_minutes DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (master_story_id, language)
);

CREATE INDEX idx_story_translations_master ON story_translations(master_story_id);
CREATE INDEX idx_story_translations_language ON story_translations(language);

-- TTS output per language (separate from translations for flexibility)
CREATE TABLE IF NOT EXISTS story_audio (
    id BIGSERIAL PRIMARY KEY,
    master_story_id BIGINT NOT NULL REFERENCES curated_stories(id) ON DELETE CASCADE,
    language VARCHAR(10) NOT NULL,
    audio_file_url VARCHAR(512) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (master_story_id, language)
);

CREATE INDEX idx_story_audio_master ON story_audio(master_story_id);
CREATE INDEX idx_story_audio_language ON story_audio(language);

-- Job status for idempotent processing and retry tracking
CREATE TABLE IF NOT EXISTS story_processing_status (
    id BIGSERIAL PRIMARY KEY,
    master_story_id BIGINT NOT NULL REFERENCES curated_stories(id) ON DELETE CASCADE,
    language VARCHAR(10) NOT NULL,
    stage VARCHAR(30) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (master_story_id, language)
);

CREATE INDEX idx_story_processing_status_master ON story_processing_status(master_story_id);
CREATE INDEX idx_story_processing_status_stage ON story_processing_status(stage);
CREATE INDEX idx_story_processing_status_updated ON story_processing_status(updated_at);
