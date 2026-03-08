-- Story safety score (0-100) for children's AI platform hardening.
-- Score computed before save; rejected if below threshold.
ALTER TABLE stories ADD COLUMN IF NOT EXISTS safety_score INT;
