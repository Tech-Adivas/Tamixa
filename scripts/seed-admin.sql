-- Seed admin user: admin@techadivas.com / Admin123!
-- Run against your tamixa_kids database (the one the backend uses).
-- Creates the user if missing, or resets password and promotes to ADMIN if exists.
--
-- PREFERRED: Use the dev API instead (backend must be running with dev profile):
--   curl -X POST http://localhost:8080/api/v1/dev/seed-admin
-- Requires SEED_ADMIN_ENABLED=true (default in dev profile).

-- BCrypt hash for password "Admin123!" (fallback if API unavailable)
INSERT INTO parents (email, password_hash, role, created_at)
VALUES (
  'admin@techadivas.com',
  '$2b$10$e/kBGc2ZnYypHBCBuyz55eDuk.6kgvLCJnuXecgKZhx.W40ax3rpG',
  'ADMIN',
  NOW()
)
ON CONFLICT (email) DO UPDATE SET
  password_hash = EXCLUDED.password_hash,
  role = 'ADMIN',
  suspended_at = NULL;

-- Verify: SELECT id, email, role FROM parents WHERE email = 'admin@techadivas.com';
