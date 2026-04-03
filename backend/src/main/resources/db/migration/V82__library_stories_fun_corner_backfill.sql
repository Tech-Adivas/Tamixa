-- Tag all active library rows for the parent app Fun corner filter (category match: Fun stories / Funny Stories).
-- One-time backfill so existing catalog appears under Library → Fun corner without manual admin edits.
UPDATE library_stories
SET category = 'Fun stories'
WHERE deleted_at IS NULL;
