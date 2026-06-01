package com.unishare.api.common.event;

import java.util.UUID;

public record BookingReviewCreatedEvent(
        UUID reviewId,
        UUID bookingId,
        UUID mentorId,
        UUID reviewerId,
        Integer rating,
        String comment
) implements DomainEvent {}
