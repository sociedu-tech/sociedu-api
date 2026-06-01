package com.unishare.api.common.event;

import java.util.UUID;

public record SessionAwaitingConfirmationEvent(
        UUID bookingId,
        UUID sessionId,
        UUID buyerId,
        UUID mentorId,
        UUID acknowledgedBy) implements DomainEvent {
}
