CREATE TABLE booking_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    mentor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    package_id UUID NOT NULL REFERENCES service_packages(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_booking_reviewer UNIQUE (booking_id, reviewer_id)
);

CREATE INDEX idx_reviews_mentor ON booking_reviews(mentor_id);
CREATE INDEX idx_reviews_package ON booking_reviews(package_id);

ALTER TABLE mentor_profiles ADD COLUMN rating_count INT NOT NULL DEFAULT 0;
ALTER TABLE mentor_profiles ADD COLUMN rating_total BIGINT NOT NULL DEFAULT 0;
ALTER TABLE mentor_profiles ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
