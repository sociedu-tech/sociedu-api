package com.unishare.api.common.event;

import java.util.UUID;

public record SessionDisputedEvent(
        UUID bookingId,
        UUID sessionId,
        UUID buyerId,
        UUID mentorId) implements DomainEvent {
}
