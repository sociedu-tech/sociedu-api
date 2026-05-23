CREATE TABLE payout_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    gross_amount DECIMAL(19, 2) NOT NULL,
    platform_fee_rate DECIMAL(5, 2) NOT NULL,
    net_amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    bank_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(255) NOT NULL,
    account_holder VARCHAR(100) NOT NULL,
    reject_reason TEXT,
    failure_reason TEXT,
    transaction_reference VARCHAR(255),
    processed_by UUID REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);

CREATE TABLE payout_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payout_request_id UUID NOT NULL REFERENCES payout_requests(id) ON DELETE CASCADE,
    actor_id UUID REFERENCES users(id),
    previous_status VARCHAR(32),
    next_status VARCHAR(32) NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payouts_mentor ON payout_requests(mentor_id);
CREATE INDEX idx_payouts_status ON payout_requests(status);
