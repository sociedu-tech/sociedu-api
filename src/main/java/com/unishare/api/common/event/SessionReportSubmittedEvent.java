package com.unishare.api.common.event;

import java.util.UUID;

public record SessionReportSubmittedEvent(
        UUID requestId,
        UUID bookingId,
        UUID mentorId,
        UUID menteeId,
        String title
) implements DomainEvent {}
