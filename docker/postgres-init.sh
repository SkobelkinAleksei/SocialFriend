#!/bin/sh
set -eu
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<EOSQL
CREATE DATABASE social_user_db;
CREATE DATABASE social_friend_db;
CREATE DATABASE social_post_db;
CREATE DATABASE social_comment_db;
CREATE DATABASE social_like_db;
CREATE DATABASE social_notification_db;
CREATE DATABASE social_chat_db;
CREATE DATABASE social_event_db;
CREATE DATABASE social_security_db;
CREATE DATABASE social_scheduler_db;
EOSQL
