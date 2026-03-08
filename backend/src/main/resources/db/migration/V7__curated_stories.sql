-- Curated stories: Tamil stories visible to all users.
-- No parent_id; created by admins via upload (text/audio).

CREATE TABLE IF NOT EXISTS curated_stories (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    content TEXT NOT NULL,
    theme VARCHAR(100) NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'ta',
    age INT NOT NULL,
    child_name VARCHAR(255) NOT NULL DEFAULT 'Child',
    word_count INT NOT NULL DEFAULT 0,
    reading_time_minutes DOUBLE PRECISION NOT NULL DEFAULT 0,
    moral TEXT,
    audio_file_url VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_curated_stories_language ON curated_stories(language);
CREATE INDEX idx_curated_stories_age ON curated_stories(age);
CREATE INDEX idx_curated_stories_theme ON curated_stories(theme);
CREATE INDEX idx_curated_stories_created_at ON curated_stories(created_at);
