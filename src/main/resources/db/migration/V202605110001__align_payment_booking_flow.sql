ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;

ALTER TABLE orders
    ALTER COLUMN status TYPE VARCHAR(50)
    USING CASE status::TEXT
        WHEN '0' THEN 'pending_payment'
        WHEN '1' THEN 'paid'
        WHEN '2' THEN 'failed'
        WHEN '3' THEN 'canceled'
        WHEN '4' THEN 'refunded'
        ELSE status::TEXT
    END;

UPDATE orders
SET status = 'pending_payment'
WHERE status IS NULL;

ALTER TABLE orders
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS provider_transaction_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS raw_response JSONB;

UPDATE payment_transactions
SET provider = 'vnpay'
WHERE provider IS NULL;

ALTER TABLE payment_transactions
    ALTER COLUMN provider SET NOT NULL,
    ALTER COLUMN status TYPE VARCHAR(50)
    USING CASE status::TEXT
        WHEN '0' THEN 'pending'
        WHEN '1' THEN 'success'
        WHEN '2' THEN 'failed'
        ELSE status::TEXT
    END;

UPDATE payment_transactions
SET status = 'pending'
WHERE status IS NULL;

ALTER TABLE payment_transactions
    ALTER COLUMN status SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_provider_txn
    ON payment_transactions (provider_transaction_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_provider_txn
    ON payment_transactions (provider_transaction_id)
    WHERE provider_transaction_id IS NOT NULL;

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

ALTER TABLE bookings
    ALTER COLUMN status TYPE VARCHAR(50)
    USING CASE status::TEXT
        WHEN '0' THEN 'pending'
        WHEN '1' THEN 'scheduled'
        WHEN '2' THEN 'in_progress'
        WHEN '3' THEN 'completed'
        WHEN '4' THEN 'canceled'
        ELSE status::TEXT
    END;

UPDATE bookings
SET status = 'pending'
WHERE status IS NULL;

UPDATE bookings
SET version = 0
WHERE version IS NULL;

ALTER TABLE bookings
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN version SET NOT NULL;

ALTER TABLE booking_sessions
    ADD COLUMN IF NOT EXISTS curriculum_id UUID,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS meeting_url TEXT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS actual_started_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS actual_ended_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS canceled_by UUID,
    ADD COLUMN IF NOT EXISTS canceled_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS cancel_reason TEXT,
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

ALTER TABLE booking_sessions
    ALTER COLUMN status TYPE VARCHAR(50)
    USING CASE status::TEXT
        WHEN '0' THEN 'pending'
        WHEN '1' THEN 'scheduled'
        WHEN '2' THEN 'in_progress'
        WHEN '3' THEN 'completed'
        WHEN '4' THEN 'canceled'
        WHEN '5' THEN 'no_show'
        ELSE status::TEXT
    END;

UPDATE booking_sessions
SET status = 'pending'
WHERE status IS NULL;

UPDATE booking_sessions
SET version = 0
WHERE version IS NULL;

ALTER TABLE booking_sessions
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN version SET NOT NULL;

CREATE TABLE IF NOT EXISTS booking_session_evidences
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_session_id UUID NOT NULL REFERENCES booking_sessions (id) ON DELETE CASCADE,
    uploaded_by        UUID NOT NULL REFERENCES users (id),
    file_id            UUID REFERENCES files (id),
    description        VARCHAR(255),
    created_at         TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_booking_session_evidence_session
    ON booking_session_evidences (booking_session_id);

CREATE TABLE IF NOT EXISTS payout_records
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id      UUID NOT NULL REFERENCES bookings (id),
    mentor_id       UUID NOT NULL REFERENCES users (id),
    source_event_id UUID NOT NULL,
    amount          DECIMAL(19, 2) NOT NULL,
    status          VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    version         BIGINT DEFAULT 0 NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_payout_records_booking
    ON payout_records (booking_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_payout_records_source_event
    ON payout_records (source_event_id);
