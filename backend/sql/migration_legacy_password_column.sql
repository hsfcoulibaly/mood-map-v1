-- NOT for new Cloud SQL / empty databases. Use schema.sql there instead.
-- Use ONLY when `DESCRIBE users` shows a column named `password` (not `password_hash`).
-- If you already applied schema.sql, this import will fail (unknown column `password`).
-- Works on MySQL 5.7+ and 8.x. Do not re-run if `password_hash` already exists.

ALTER TABLE users CHANGE COLUMN password password_hash VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN google_sub VARCHAR(255) NULL AFTER password_hash;
CREATE UNIQUE INDEX uq_users_google_sub ON users (google_sub);
