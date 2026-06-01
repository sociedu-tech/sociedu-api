-- Chạy SAU Hibernate (spring.jpa.defer-datasource-initialization=true).
-- Bổ sung extension + bảng roles khi DB remote thiếu bảng mới dù schema cũ chưa đồng bộ.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS roles
(
    id   UUID         NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
);

-- ============================================================
-- Migration: Thêm scheduled_at_end vào booking_sessions
-- ============================================================
ALTER TABLE booking_sessions ADD COLUMN IF NOT EXISTS scheduled_at_end TIMESTAMP WITH TIME ZONE;

-- ============================================================
-- Migration: Thêm progress_percent vào bookings
-- ============================================================
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS progress_percent INTEGER;

-- ============================================================
-- Migration: Tạo bảng session_report_requests
-- ============================================================
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
    status                VARCHAR(50)              NOT NULL DEFAULT 'PENDING_SUBMISSION',
    mentee_content        TEXT,
    mentee_attachment_url TEXT,
    mentor_feedback       TEXT,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_srr_booking_id  ON session_report_requests (booking_id);
CREATE INDEX IF NOT EXISTS idx_srr_mentor_id   ON session_report_requests (mentor_id);
CREATE INDEX IF NOT EXISTS idx_srr_mentee_id   ON session_report_requests (mentee_id);
CREATE INDEX IF NOT EXISTS idx_srr_status      ON session_report_requests (status);
