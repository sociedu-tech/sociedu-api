package com.unishare.api.modules.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SessionReportRequestResponse {
    private UUID id;
    private UUID bookingId;
    private UUID sessionId;
    private UUID mentorId;
    private UUID menteeId;
    private String title;
    private String description;
    private Instant dueDate;
    private String status;
    private String menteeContent;
    private String menteeAttachmentUrl;
    private String mentorFeedback;
    private Instant createdAt;
    private Instant updatedAt;
}
