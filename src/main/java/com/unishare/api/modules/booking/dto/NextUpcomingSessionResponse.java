package com.unishare.api.modules.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NextUpcomingSessionResponse {
    private UUID bookingId;
    private UUID sessionId;
    private String title;
    private Instant scheduledAt;
    private String status;
    private UUID counterpartyId;
    private String counterpartyName;
}
