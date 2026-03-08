-- Parent suspend: admin can suspend accounts.
ALTER TABLE parents ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_parents_suspended_at ON parents(suspended_at) WHERE suspended_at IS NOT NULL;
