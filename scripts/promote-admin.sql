-- Promote admin@techadivas.com to ADMIN role (user must already exist)
-- Run against your tamixa_kids database (the one the backend uses)
-- If the account doesn't exist or you forgot the password, use seed-admin.sql instead.

UPDATE parents SET role = 'ADMIN' WHERE email = 'admin@techadivas.com';

-- Verify: SELECT id, email, role FROM parents WHERE email = 'admin@techadivas.com';
