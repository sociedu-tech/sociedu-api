package com.unishare.api.common.event;

import java.util.UUID;

public record SessionCompletedEvent(
        UUID bookingId,
        UUID sessionId,
        UUID buyerId,
        UUID mentorId,
        String sessionTitle
) implements DomainEvent {}
