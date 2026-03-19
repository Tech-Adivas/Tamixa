-- Add review states: CHANGES_REQUESTED, REJECTED.
-- review_notes stores reviewer feedback when requesting changes or rejecting.
ALTER TABLE library_stories ADD COLUMN IF NOT EXISTS review_notes TEXT;

-- Ensure status column allows new values (VARCHAR(20) already)
-- Valid statuses: DRAFT, PUBLISHED, PROCESSING, READY, CHANGES_REQUESTED, REJECTED
