package com.unishare.api.modules.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BookingSessionResponse {
    private UUID id;
    private UUID curriculumId;
    private String title;
    private Instant scheduledAt;
    private Instant scheduledAtEnd;
    private Instant completedAt;
    private String status;
    private String meetingUrl;
    private Boolean menteeCompletionAck;
    private Boolean mentorCompletionAck;
    private Instant menteeAckAt;
    private Instant mentorAckAt;
    private List<EvidenceResponse> evidences;
}
