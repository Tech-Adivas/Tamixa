-- Admin marks story for reject from language view (enables Reject button). Persisted across refresh.
ALTER TABLE library_stories ADD COLUMN IF NOT EXISTS reject_marked_at TIMESTAMPTZ NULL;
