-- Gate "Regenerate with prompt" for content managers; SUPER_ADMIN/ADMIN bypass (unlock flow for others).
ALTER TABLE library_stories ADD COLUMN IF NOT EXISTS regenerate_prompt_locked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE library_stories ADD COLUMN IF NOT EXISTS regenerate_prompt_lock_approved BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE library_stories ADD COLUMN IF NOT EXISTS regenerate_prompt_unlock_requested_at TIMESTAMPTZ NULL;
