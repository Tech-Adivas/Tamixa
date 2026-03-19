-- Short-form content: riddles, thought for the day, proverbs, tongue twisters, etc.
-- Served to app by type and language; optional display_date for "daily" items.

CREATE TABLE IF NOT EXISTS short_content (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    answer TEXT,
    language VARCHAR(10) NOT NULL DEFAULT 'ta',
    age_min INT,
    age_max INT,
    display_date DATE,
    audio_url VARCHAR(512),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_short_content_type_language ON short_content(type, language);
CREATE INDEX idx_short_content_display_date ON short_content(display_date, type);
CREATE INDEX idx_short_content_status ON short_content(status);
CREATE INDEX idx_short_content_created_at ON short_content(created_at);

COMMENT ON TABLE short_content IS 'Riddles, thought for the day, proverbs, tongue twisters, jokes, etc.';
COMMENT ON COLUMN short_content.type IS 'RIDDLE, THOUGHT_FOR_THE_DAY, PROVERB, TONGUE_TWISTER, JOKE, FUN_FACT, etc.';
COMMENT ON COLUMN short_content.answer IS 'Answer or punchline (e.g. for riddles, jokes)';
COMMENT ON COLUMN short_content.display_date IS 'Optional: for daily content (e.g. one thought per day)';
