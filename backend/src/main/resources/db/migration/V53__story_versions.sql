-- Story versioning: audit trail for library story content changes.
-- Each content update creates a new version; library_stories holds current.
CREATE TABLE IF NOT EXISTS story_versions (
    id BIGSERIAL PRIMARY KEY,
    library_story_id BIGINT NOT NULL REFERENCES library_stories(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    title VARCHAR(255),
    content TEXT NOT NULL,
    theme VARCHAR(100) NOT NULL,
    moral TEXT,
    word_count INT NOT NULL DEFAULT 0,
    reading_time_minutes DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255)
);

CREATE INDEX idx_story_versions_library_story ON story_versions(library_story_id);
CREATE INDEX idx_story_versions_created ON story_versions(created_at DESC);
CREATE UNIQUE INDEX idx_story_versions_story_version ON story_versions(library_story_id, version_number);

COMMENT ON TABLE story_versions IS 'Audit trail of library story content; new row on each edit';
