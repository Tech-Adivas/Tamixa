-- Fingerprint of title+content+moral+narration script at "Have reviewed" time; used to flag stale reviews after edits.
ALTER TABLE library_story_language_reviews
  ADD COLUMN IF NOT EXISTS content_fingerprint VARCHAR(64) NULL;

COMMENT ON COLUMN library_story_language_reviews.content_fingerprint IS 'SHA-256 hex of normalized story fields at review time; mismatch => content changed since review';
