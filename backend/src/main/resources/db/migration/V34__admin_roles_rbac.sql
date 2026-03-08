-- Extend role column for new org-level admin roles.
-- Migrate existing ADMIN to SUPER_ADMIN.
ALTER TABLE parents ALTER COLUMN role TYPE VARCHAR(30);
UPDATE parents SET role = 'SUPER_ADMIN' WHERE role = 'ADMIN';
