-- Run when `users` already has `password_hash` but not `google_sub` (MySQL 1054 on google_sub).
-- If your table has a column named `password` instead, run migration_legacy_password_column.sql first.
-- MySQL 8.0.12+ recommended (IF NOT EXISTS). On older MySQL, run ALTER/CREATE manually.

ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_sub VARCHAR(255) NULL AFTER password_hash;
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_google_sub ON users (google_sub);
