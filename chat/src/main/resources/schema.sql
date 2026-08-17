ALTER TABLE IF EXISTS chat_participants
    ADD COLUMN IF NOT EXISTS admin boolean NOT NULL DEFAULT false;
