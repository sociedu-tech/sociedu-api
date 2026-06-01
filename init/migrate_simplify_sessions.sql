-- Bỏ báo cáo tiến độ và thêm xác nhận hoàn thành hai bên cho buổi học.

DROP TABLE IF EXISTS mentee_progress_reports CASCADE;
DROP TABLE IF EXISTS progress_reports CASCADE;

ALTER TABLE booking_sessions
    ADD COLUMN IF NOT EXISTS mentee_completion_ack BOOLEAN,
    ADD COLUMN IF NOT EXISTS mentor_completion_ack BOOLEAN,
    ADD COLUMN IF NOT EXISTS mentee_ack_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS mentor_ack_at TIMESTAMP;

COMMENT ON COLUMN booking_sessions.mentee_completion_ack IS 'NULL=chưa phản hồi, TRUE=đồng ý hoàn thành, FALSE=từ chối';
COMMENT ON COLUMN booking_sessions.mentor_completion_ack IS 'NULL=chưa phản hồi, TRUE=đồng ý hoàn thành, FALSE=từ chối';
