-- Per-language review flag for Story for review flow.
-- Admin marks languages as reviewed before Approve is enabled; persisted across refresh.
CREATE TABLE library_story_language_reviews (
  id BIGSERIAL PRIMARY KEY,
  library_story_id BIGINT NOT NULL REFERENCES library_stories(id) ON DELETE CASCADE,
  language VARCHAR(10) NOT NULL,
  reviewed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (library_story_id, language)
);

CREATE INDEX idx_library_story_language_reviews_story ON library_story_language_reviews(library_story_id);
