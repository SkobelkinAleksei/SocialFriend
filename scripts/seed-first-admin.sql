-- Первый админ MyRaion (локально).
-- 1) Запустите user и security хотя бы один раз, чтобы Hibernate создал таблицы.
-- 2) Заполните пять строк ниже (пароль: минимум 8 символов, заглавная, строчная и цифра).
-- 3) Выполните файл в psql:
--    psql -h localhost -U postgres -d postgres -v ON_ERROR_STOP=1 -f scripts/seed-first-admin.sql
-- Перед выгрузкой на сервер этот способ заменим на нормальный bootstrap.

\set ON_ERROR_STOP on

SELECT
  'CHANGE_ME_EMAIL'        AS admin_email,
  'CHANGE_ME_PASSWORD'     AS admin_password,
  'CHANGE_ME_FIRST_NAME'   AS admin_first_name,
  'CHANGE_ME_LAST_NAME'    AS admin_last_name,
  '+7CHANGE_ME000'         AS admin_phone
\gset

SELECT CASE
  WHEN :'admin_email' LIKE 'CHANGE_ME%'
    OR :'admin_password' LIKE 'CHANGE_ME%'
    OR :'admin_first_name' LIKE 'CHANGE_ME%'
    OR :'admin_last_name' LIKE 'CHANGE_ME%'
    OR :'admin_phone' LIKE '%CHANGE_ME%'
  THEN 1 / 0
  ELSE 1
END AS filled;

\c social_user_db

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS platform_role varchar(20);
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS account_status varchar(20);

SELECT crypt(:'admin_password', gen_salt('bf', 10)) AS pwd_hash \gset

INSERT INTO users (
  first_name, last_name, email, number_phone, password, birthday,
  city, street_address, district_name, home_latitude, home_longitude,
  reputation, time_stamp, bio, account_status, platform_role
)
SELECT
  :'admin_first_name',
  :'admin_last_name',
  lower(:'admin_email'),
  :'admin_phone',
  :'pwd_hash',
  DATE '1990-01-01',
  'Служебный',
  'служебный аккаунт',
  'Служебный',
  55.751244,
  37.618423,
  0,
  NOW()::timestamp(0),
  'Админ района',
  'ACTIVE',
  'ADMIN'
WHERE NOT EXISTS (
  SELECT 1 FROM users WHERE lower(email) = lower(:'admin_email')
);

UPDATE users
SET platform_role = 'ADMIN',
    account_status = COALESCE(account_status, 'ACTIVE')
WHERE lower(email) = lower(:'admin_email');

INSERT INTO user_settings (
  user_id, search_radius, allow_dm_from_all, allow_comments_from_all,
  photo_visibility, notify_comments, notify_messages, notify_event_requests,
  notify_reputation, show_last_seen
)
SELECT u.id, 1.0, TRUE, TRUE, 'ALL', TRUE, TRUE, TRUE, TRUE, TRUE
FROM users u
WHERE lower(u.email) = lower(:'admin_email')
  AND NOT EXISTS (SELECT 1 FROM user_settings s WHERE s.user_id = u.id);

SELECT id AS admin_id FROM users WHERE lower(email) = lower(:'admin_email') \gset

\c social_security_db

ALTER TABLE IF EXISTS users_security ADD COLUMN IF NOT EXISTS platform_role varchar(20);
ALTER TABLE IF EXISTS users_security ADD COLUMN IF NOT EXISTS account_status varchar(20);
ALTER TABLE IF EXISTS users_security ADD COLUMN IF NOT EXISTS enabled boolean DEFAULT true;
ALTER TABLE IF EXISTS users_security ADD COLUMN IF NOT EXISTS failed_attempts integer NOT NULL DEFAULT 0;

INSERT INTO users_security (
  id, username, password, failed_attempts, enabled, account_status, platform_role, created_at, updated_at
)
VALUES (
  :admin_id,
  lower(:'admin_email'),
  :'pwd_hash',
  0,
  TRUE,
  'ACTIVE',
  'ADMIN',
  NOW()::timestamp(0),
  NOW()::timestamp(0)
)
ON CONFLICT (id) DO UPDATE
SET username = excluded.username,
    password = excluded.password,
    enabled = TRUE,
    account_status = 'ACTIVE',
    platform_role = 'ADMIN',
    updated_at = NOW()::timestamp(0);

SELECT 'Админ готов. id=' || :admin_id || ' email=' || lower(:'admin_email') AS result;
