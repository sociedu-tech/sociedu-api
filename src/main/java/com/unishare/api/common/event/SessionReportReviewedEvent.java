package com.unishare.api.common.event;

import java.util.UUID;

public record SessionReportReviewedEvent(
        UUID requestId,
        UUID bookingId,
        UUID mentorId,
        UUID menteeId,
        String title,
        String status
) implements DomainEvent {}
