package com.unishare.api.common.event;

import java.time.Instant;
import java.util.UUID;

public record SessionScheduledEvent(
        UUID bookingId,
        UUID sessionId,
        UUID buyerId,
        UUID mentorId,
        Instant scheduledAt,
        String sessionTitle
) implements DomainEvent {}
