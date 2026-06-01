-- ================================================================
-- Migration: 2026-06-01 — Add scheduled_at_end, progress_percent,
--            and session_report_requests table
-- ================================================================
-- Đã tích hợp vào schema.sql (chạy tự động khi khởi động).
-- File này chỉ để lưu lịch sử thay đổi schema.
-- ================================================================

-- 1. Thêm thời gian kết thúc buổi học
ALTER TABLE booking_sessions ADD COLUMN IF NOT EXISTS scheduled_at_end TIMESTAMP WITH TIME ZONE;

-- 2. Mentor cập nhật tiến trình gói dịch vụ (override auto-computed)
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS progress_percent INTEGER;

-- 3. Bảng yêu cầu nộp báo cáo từ mentor
CREATE TABLE IF NOT EXISTS session_report_requests
(
    id                    UUID                     NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id            UUID                     NOT NULL,
    session_id            UUID,
    mentor_id             UUID                     NOT NULL,
    mentee_id             UUID                     NOT NULL,
    title                 VARCHAR(255)             NOT NULL,
    description           TEXT,
    due_date              TIMESTAMP WITH TIME ZONE,
    -- PENDING_SUBMISSION | SUBMITTED | APPROVED | REJECTED
    status                VARCHAR(50)              NOT NULL DEFAULT 'PENDING_SUBMISSION',
    mentee_content        TEXT,
    mentee_attachment_url TEXT,
    mentor_feedback       TEXT,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_srr_booking_id ON session_report_requests (booking_id);
CREATE INDEX IF NOT EXISTS idx_srr_mentor_id  ON session_report_requests (mentor_id);
CREATE INDEX IF NOT EXISTS idx_srr_mentee_id  ON session_report_requests (mentee_id);
CREATE INDEX IF NOT EXISTS idx_srr_status     ON session_report_requests (status);
