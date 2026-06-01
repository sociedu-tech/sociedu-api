-- ==========================================
-- EXTENSIONS
-- ==========================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==========================================
-- AUTH & USER MANAGEMENT
-- ==========================================
CREATE TABLE roles
(
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE users
(
    id             UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    email          VARCHAR(255) UNIQUE,
    email_verified BOOLEAN                  DEFAULT FALSE,
    phone_number   VARCHAR(20) UNIQUE,
    phone_verified BOOLEAN                  DEFAULT FALSE,
    date_of_birth  DATE,
    status         VARCHAR(32)              DEFAULT 'pending',
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE user_roles
(
    user_id UUID REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE user_credentials
(
    user_id       UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    password_hash TEXT NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE otp_tokens
(
    id         UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code       VARCHAR(128) NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used       BOOLEAN                  DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE refresh_tokens
(
    id             UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token          VARCHAR(512) UNIQUE      NOT NULL,
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked        BOOLEAN                  DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_used_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    replaced_by_id UUID,
    device_info    VARCHAR(255),
    ip_address     VARCHAR(64),
    user_agent     VARCHAR(512)
);

CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);
CREATE UNIQUE INDEX ux_refresh_tokens_token ON refresh_tokens (token);

-- ==========================================
-- FILES
-- ==========================================
CREATE TABLE files
(
    id               UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    uploader_id      UUID REFERENCES users (id),
    file_name        VARCHAR(255) NOT NULL,
    file_url         TEXT         NOT NULL,
    public_id        TEXT,
    resource_type    VARCHAR(20),
    mime_type        VARCHAR(100) NOT NULL,
    file_size        BIGINT       NOT NULL,
    storage_provider VARCHAR(50)  NOT NULL DEFAULT 'cloudinary',
    visibility       VARCHAR(32)  NOT NULL DEFAULT 'PRIVATE',
    entity_type      VARCHAR(50),
    entity_id        UUID,
    deleted_at       TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_files_uploader ON files (uploader_id);
CREATE INDEX idx_files_entity ON files (entity_type, entity_id);

-- ==========================================
-- USER PROFILE & RELATED LOOKUPS
-- ==========================================
CREATE TABLE user_profiles
(
    user_id        UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    first_name     VARCHAR(50),
    last_name      VARCHAR(50),
    headline       VARCHAR(150),
    avatar_file_id UUID REFERENCES files (id),
    bio            TEXT,
    location       VARCHAR(255),
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE universities
(
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE fields_of_study
(
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) UNIQUE
);

CREATE TABLE user_educations
(
    id            UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    university_id UUID REFERENCES universities (id),
    major_id      UUID REFERENCES fields_of_study (id),
    degree        VARCHAR(255),
    start_date    DATE,
    end_date      DATE,
    is_current    BOOLEAN                  DEFAULT FALSE,
    description   TEXT,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_user_educations_user ON user_educations (user_id);

CREATE TABLE user_experiences
(
    id          UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    company     VARCHAR(255),
    position    VARCHAR(255),
    start_date  DATE,
    end_date    DATE,
    is_current  BOOLEAN                  DEFAULT FALSE,
    description TEXT
);

CREATE INDEX idx_user_experience_user ON user_experiences (user_id);

CREATE TABLE user_certificates
(
    id                 UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name               VARCHAR(255),
    organization       VARCHAR(255),
    issue_date         DATE,
    expiration_date    DATE,
    credential_file_id UUID REFERENCES files (id),
    description        TEXT
);

CREATE INDEX idx_user_cert_user ON user_certificates (user_id);

CREATE TABLE user_languages
(
    id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id  UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    language VARCHAR(100) NOT NULL,
    level    VARCHAR(50)
);

CREATE TABLE user_projects
(
    id          UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(255),
    description TEXT,
    project_url TEXT,
    file_id     UUID REFERENCES files (id),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);


CREATE TABLE skills
(
    id         UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    name       VARCHAR(255) UNIQUE NOT NULL,
    category   VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE user_skills
(
    user_id             UUID REFERENCES users (id) ON DELETE CASCADE,
    skill_id            UUID REFERENCES skills (id) ON DELETE CASCADE,
    level               VARCHAR(50),
    years_of_experience INT,
    PRIMARY KEY (user_id, skill_id)
);

-- ==========================================
-- MENTOR MODULE
-- ==========================================
CREATE TABLE mentor_profiles
(
    user_id             UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    headline            TEXT,
    expertise           TEXT,
    base_price          DECIMAL(19, 2),
    rating_avg          REAL                     DEFAULT 0,
    rating_count        INT                      DEFAULT 0,
    rating_total        BIGINT                   DEFAULT 0,
    version             BIGINT                   DEFAULT 0,
    sessions_completed  INT                      DEFAULT 0,
    verification_status VARCHAR(32)              DEFAULT 'pending',
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_mentor_profiles_verification ON mentor_profiles (verification_status);

/* Mentor applications (from entity MentorApplication) */
CREATE TABLE mentor_requests
(
    id                  UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    user_id             UUID           NOT NULL REFERENCES users (id),
    status              VARCHAR(64)    NOT NULL DEFAULT 'SUBMITTED',
    headline            TEXT,
    bio                 TEXT,
    expertise           JSONB,
    years_of_experience INT,
    hourly_rate         DECIMAL(19,2),
    cv_file_id          UUID,
    cv_url              TEXT,
    portfolio_urls      JSONB,
    certificates        JSONB,
    reason              TEXT,
    note                TEXT,
    reviewed_by         UUID,
    reviewed_at         TIMESTAMP WITH TIME ZONE,
    resubmit_count      INT DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_mentor_requests_user ON mentor_requests (user_id);

-- ==========================================
-- SERVICE & PACKAGE
-- ==========================================
CREATE TABLE service_packages
(
    id          UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    mentor_id   UUID         NOT NULL REFERENCES users (id),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    is_active   BOOLEAN                  DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_package_mentor ON service_packages (mentor_id);

CREATE TABLE service_package_versions
(
    id            UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    package_id    UUID           NOT NULL REFERENCES service_packages (id) ON DELETE CASCADE,
    price         DECIMAL(19, 2) NOT NULL,
    duration      INT            NOT NULL,
    delivery_type VARCHAR(255),
    is_default    BOOLEAN                  DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    version       BIGINT                   DEFAULT 0,
    deleted_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_package_versions_package ON service_package_versions (package_id);

CREATE TABLE package_curriculums
(
    id                 UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    package_version_id UUID         NOT NULL REFERENCES service_package_versions (id) ON DELETE CASCADE,
    title              VARCHAR(255) NOT NULL,
    description        TEXT,
    order_index        INT          NOT NULL,
    duration           INT,
    created_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ==========================================
-- ORDER & PAYMENT
-- ==========================================
CREATE TABLE orders
(
    id           UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    buyer_id     UUID           NOT NULL REFERENCES users (id),
    service_id   UUID           NOT NULL REFERENCES service_package_versions (id),
    status       VARCHAR(50)    NOT NULL  DEFAULT 'pending_payment',
    total_amount DECIMAL(10, 2) NOT NULL,
    paid_at      TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_orders_buyer ON orders (buyer_id);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE payment_transactions
(
    id                      UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    order_id                UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    provider                VARCHAR(50)    NOT NULL  DEFAULT 'vnpay',
    provider_transaction_id VARCHAR(255),
    amount                  DECIMAL(19, 2) NOT NULL,
    status                  VARCHAR(50)    NOT NULL  DEFAULT 'pending',
    raw_response            JSONB,
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_payment_order ON payment_transactions (order_id);
CREATE INDEX idx_payment_provider_txn ON payment_transactions (provider_transaction_id);
CREATE UNIQUE INDEX ux_payment_provider_txn ON payment_transactions (provider_transaction_id) WHERE provider_transaction_id IS NOT NULL;

-- ==========================================
-- BOOKING MODULE
-- ==========================================
CREATE TABLE bookings
(
    id               UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    order_id         UUID UNIQUE REFERENCES orders (id),
    buyer_id         UUID        NOT NULL REFERENCES users (id),
    mentor_id        UUID        NOT NULL REFERENCES users (id),
    package_id       UUID        NOT NULL REFERENCES service_packages (id),
    status           VARCHAR(50) NOT NULL     DEFAULT 'pending',
    progress_percent INT,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    version          BIGINT      NOT NULL     DEFAULT 0
);

CREATE INDEX idx_booking_buyer ON bookings (buyer_id);
CREATE INDEX idx_booking_mentor ON bookings (mentor_id);

CREATE TABLE booking_sessions
(
    id                    UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    booking_id            UUID        NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    curriculum_id         UUID,
    title                 VARCHAR(255),
    status                VARCHAR(50) NOT NULL     DEFAULT 'pending',
    scheduled_at          TIMESTAMP WITH TIME ZONE,
    scheduled_at_end      TIMESTAMP WITH TIME ZONE,
    completed_at          TIMESTAMP WITH TIME ZONE,
    meeting_url           TEXT,
    created_at            TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    actual_started_at     TIMESTAMP WITH TIME ZONE,
    actual_ended_at       TIMESTAMP WITH TIME ZONE,
    canceled_by           UUID,
    canceled_at           TIMESTAMP WITH TIME ZONE,
    cancel_reason         TEXT,
    mentee_completion_ack BOOLEAN,
    mentor_completion_ack BOOLEAN,
    mentee_ack_at         TIMESTAMP WITH TIME ZONE,
    mentor_ack_at         TIMESTAMP WITH TIME ZONE,
    version               BIGINT      NOT NULL     DEFAULT 0
);

CREATE INDEX idx_session_booking ON booking_sessions (booking_id);

CREATE TABLE booking_session_evidences
(
    id                 UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    booking_session_id UUID NOT NULL REFERENCES booking_sessions (id) ON DELETE CASCADE,
    uploaded_by        UUID NOT NULL REFERENCES users (id),
    file_id            UUID REFERENCES files (id),
    description        VARCHAR(255),
    created_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

/* Reviews for bookings (from entity BookingReview) */
CREATE TABLE booking_reviews
(
    id           UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    booking_id   UUID           NOT NULL REFERENCES bookings (id),
    mentor_id    UUID           NOT NULL REFERENCES users (id),
    package_id   UUID           NOT NULL REFERENCES service_packages (id),
    reviewer_id  UUID           NOT NULL REFERENCES users (id),
    rating       INT            NOT NULL,
    comment      TEXT,
    version      BIGINT,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    edited_at    TIMESTAMP WITH TIME ZONE,
    deleted_at   TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_booking_reviews_booking ON booking_reviews (booking_id);

CREATE TABLE session_report_requests
(
    id                    UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    booking_id            UUID                     NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    session_id            UUID                     REFERENCES booking_sessions (id) ON DELETE SET NULL,
    mentor_id             UUID                     NOT NULL REFERENCES users (id),
    mentee_id             UUID                     NOT NULL REFERENCES users (id),
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

CREATE INDEX idx_srr_booking_id ON session_report_requests (booking_id);
CREATE INDEX idx_srr_mentor_id ON session_report_requests (mentor_id);
CREATE INDEX idx_srr_mentee_id ON session_report_requests (mentee_id);
CREATE INDEX idx_srr_status ON session_report_requests (status);

-- ==========================================
-- FINANCE & PAYOUT
-- ==========================================
/* Payout requests (from entity PayoutRequest) */
CREATE TABLE payout_requests
(
    id                 UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    mentor_id          UUID           NOT NULL REFERENCES users (id),
    gross_amount       DECIMAL(19,2) NOT NULL,
    platform_fee_rate  DECIMAL(19,2) NOT NULL,
    net_amount         DECIMAL(19,2) NOT NULL,
    status             VARCHAR(32)    NOT NULL DEFAULT 'PENDING',
    bank_name          VARCHAR(100),
    account_number     VARCHAR(255),
    account_holder     VARCHAR(100),
    reject_reason      TEXT,
    failure_reason     TEXT,
    transaction_reference VARCHAR(255),
    processed_by       UUID,
    version            BIGINT,
    created_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed_at       TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_payout_requests_mentor ON payout_requests (mentor_id);

/* Payout audit logs (from entity PayoutAuditLog) */
CREATE TABLE payout_audit_logs
(
    id                UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    payout_request_id UUID           NOT NULL,
    actor_id          UUID,
    previous_status   VARCHAR(32),
    next_status       VARCHAR(32)    NOT NULL,
    reason            TEXT,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_payout_audit_payout ON payout_audit_logs (payout_request_id);

CREATE TABLE payout_records
(
    id              UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    booking_id      UUID           NOT NULL REFERENCES bookings (id),
    mentor_id       UUID           NOT NULL REFERENCES users (id),
    source_event_id UUID           NOT NULL,
    amount          DECIMAL(19, 2) NOT NULL,
    status          VARCHAR(255)   NOT NULL  DEFAULT 'PENDING',
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    version         BIGINT         NOT NULL  DEFAULT 0
);

CREATE UNIQUE INDEX ux_payout_records_booking ON payout_records (booking_id);
CREATE UNIQUE INDEX ux_payout_records_source_event ON payout_records (source_event_id);

-- ==========================================
-- MESSAGING
-- ==========================================
CREATE TABLE conversations
(
    id         UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    type       VARCHAR(50)  NOT NULL DEFAULT 'general',
    booking_id UUID REFERENCES bookings (id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE conversation_participants
(
    conversation_id UUID REFERENCES conversations (id) ON DELETE CASCADE,
    user_id         UUID REFERENCES users (id) ON DELETE CASCADE,
    joined_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_read_at    TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE messages
(
    id              UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    conversation_id UUID         NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id       UUID         NOT NULL REFERENCES users (id),
    content         TEXT,
    type            VARCHAR(50)  NOT NULL DEFAULT 'TEXT',
    is_edited       BOOLEAN                  DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE,
    context_type    VARCHAR(100),
    context_id      UUID
);

CREATE INDEX idx_messages_conv ON messages (conversation_id);

CREATE TABLE message_attachments
(
    id         UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    message_id UUID         NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    file_id    UUID REFERENCES files (id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ==========================================
-- NOTIFICATIONS
-- ==========================================
CREATE TABLE device_tokens
(
    id           UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    user_id      UUID           NOT NULL REFERENCES users (id),
    token        VARCHAR(512)   NOT NULL UNIQUE,
    platform     VARCHAR(50)    NOT NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_device_tokens_user ON device_tokens (user_id);

CREATE TABLE notifications
(
    id             UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    user_id        UUID    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type           VARCHAR NOT NULL,
    reference_type VARCHAR,
    reference_id   UUID,
    is_read        BOOLEAN                  DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    title          VARCHAR,
    content        TEXT,
    metadata       JSONB,
    read_at        TIMESTAMP WITH TIME ZONE,
    push_status    VARCHAR(32)              DEFAULT 'PENDING',
    version        BIGINT                   DEFAULT 0
);

CREATE INDEX idx_notifications_user ON notifications (user_id);
CREATE INDEX idx_notifications_is_read ON notifications (is_read);

-- ==========================================
-- REPORTING & MODERATION
-- ==========================================
CREATE TABLE disputes
(
    id               UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    report_id        UUID,
    booking_id       UUID,
    session_id       UUID,
    raised_by        UUID           NOT NULL REFERENCES users (id),
    reason           TEXT           NOT NULL,
    description      TEXT,
    status           VARCHAR(32)    NOT NULL DEFAULT 'OPEN',
    resolution_note  TEXT,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    resolved_at      TIMESTAMP WITH TIME ZONE,
    resolved_by      UUID
);

CREATE INDEX idx_disputes_booking ON disputes (booking_id);

CREATE TABLE reports
(
    id               UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    reporter_id      UUID         NOT NULL REFERENCES users (id),
    reported_user_id UUID REFERENCES users (id),
    type             VARCHAR(100) NOT NULL,
    entity_id        UUID         NOT NULL,
    reason           TEXT         NOT NULL,
    description      TEXT,
    status           VARCHAR(50)  NOT NULL DEFAULT 'OPEN',
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    resolved_at      TIMESTAMP WITH TIME ZONE,
    resolved_by      UUID,
    resolution_note  TEXT
);

CREATE TABLE report_evidences
(
    id          UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    report_id   UUID         NOT NULL REFERENCES reports (id) ON DELETE CASCADE,
    file_id     UUID         NOT NULL REFERENCES files (id),
    description TEXT,
    uploaded_by UUID         NOT NULL REFERENCES users (id),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ==========================================
-- STATISTICS (PRE-COMPUTE)
-- ==========================================
CREATE TABLE user_stats
(
    user_id            UUID PRIMARY KEY REFERENCES users (id),
    total_orders       INT                      DEFAULT 0,
    total_spent        DECIMAL(19, 2)           DEFAULT 0,
    total_sessions     INT                      DEFAULT 0,
    completed_sessions INT                      DEFAULT 0,
    updated_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE mentor_stats
(
    mentor_id      UUID PRIMARY KEY REFERENCES users (id),
    total_students INT                      DEFAULT 0,
    total_sessions INT                      DEFAULT 0,
    total_revenue  DECIMAL(19, 2)           DEFAULT 0,
    rating_avg     REAL                     DEFAULT 0,
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE service_stats
(
    package_id    UUID PRIMARY KEY REFERENCES service_packages (id),
    total_orders  INT                      DEFAULT 0,
    total_revenue DECIMAL(19, 2)           DEFAULT 0,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE system_stats
(
    date               DATE PRIMARY KEY,
    new_users          INT                      DEFAULT 0,
    active_users       INT                      DEFAULT 0,
    total_orders       INT                      DEFAULT 0,
    successful_orders  INT                      DEFAULT 0,
    failed_orders      INT                      DEFAULT 0,
    revenue            DECIMAL(19, 2)           DEFAULT 0,
    total_sessions     INT                      DEFAULT 0,
    completed_sessions INT                      DEFAULT 0,
    new_mentors        INT                      DEFAULT 0,
    created_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ==========================================
-- EVENT LOG (ANALYTICS)
-- ==========================================
CREATE TABLE system_events
(
    id         UUID PRIMARY KEY         DEFAULT gen_random_uuid(),
    event_type SMALLINT,
    user_id    UUID,
    entity_id  UUID,
    metadata   JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_events_type ON system_events (event_type);
CREATE INDEX idx_events_user ON system_events (user_id);