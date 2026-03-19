-- Add nickname and simple profile fields for display across the app.
-- Nickname is shown instead of email/phone when set.

ALTER TABLE parents ADD COLUMN IF NOT EXISTS nickname VARCHAR(100);
ALTER TABLE parents ADD COLUMN IF NOT EXISTS display_name VARCHAR(100);
