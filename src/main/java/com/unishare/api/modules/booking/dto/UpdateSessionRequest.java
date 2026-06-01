package com.unishare.api.modules.booking.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class UpdateSessionRequest {
    private Instant scheduledAt;
    private Instant scheduledAtEnd;
    private String meetingUrl;
    private String status;
    private String cancelReason;
}
