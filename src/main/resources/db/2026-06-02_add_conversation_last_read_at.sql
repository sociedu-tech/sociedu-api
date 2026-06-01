-- Track per-user read cursor for chat unread counts
ALTER TABLE conversation_participants
    ADD COLUMN IF NOT EXISTS last_read_at TIMESTAMP;
