ALTER TABLE IF EXISTS refresh_tokens
    ADD COLUMN IF NOT EXISTS family_id varchar(36);

UPDATE refresh_tokens
SET family_id = md5(random()::text || id::text || clock_timestamp()::text)
WHERE family_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_refresh_token_family ON refresh_tokens (family_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_tokens (user_id);

ALTER TABLE IF EXISTS users_security
    ADD COLUMN IF NOT EXISTS failed_attempts integer NOT NULL DEFAULT 0;

ALTER TABLE IF EXISTS users_security
    ADD COLUMN IF NOT EXISTS locked_until timestamp;

ALTER TABLE IF EXISTS users_security
    ADD COLUMN IF NOT EXISTS enabled boolean DEFAULT true;

UPDATE users_security SET enabled = true WHERE enabled IS NULL;

ALTER TABLE IF EXISTS users_security
    ADD COLUMN IF NOT EXISTS account_status varchar(20) DEFAULT 'ACTIVE';

UPDATE users_security SET account_status = 'ACTIVE' WHERE account_status IS NULL;

ALTER TABLE IF EXISTS users_security
    ADD COLUMN IF NOT EXISTS platform_role varchar(20);

UPDATE users_security SET platform_role = 'USER' WHERE platform_role IS NULL;

UPDATE users_security
SET username = lower(username)
WHERE username IS NOT NULL AND username <> lower(username);
