-- Transcript from Google Cloud Speech-to-Text for story play screen (karaoke-style captions).
-- One row per (library_story_id, language, voice_profile); optional word-level timings for sync.
CREATE TABLE story_transcript (
    id BIGSERIAL PRIMARY KEY,
    library_story_id BIGINT NOT NULL REFERENCES library_stories(id) ON DELETE CASCADE,
    language VARCHAR(10) NOT NULL,
    voice_profile VARCHAR(64) NOT NULL DEFAULT 'default',
    transcript_text TEXT,
    word_timings_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (library_story_id, language, voice_profile)
);

CREATE INDEX idx_story_transcript_library_story ON story_transcript(library_story_id);
CREATE INDEX idx_story_transcript_lang_voice ON story_transcript(library_story_id, language, voice_profile);

COMMENT ON TABLE story_transcript IS 'Cached transcript from Google Cloud Speech-to-Text for playback captions and word-level sync';
