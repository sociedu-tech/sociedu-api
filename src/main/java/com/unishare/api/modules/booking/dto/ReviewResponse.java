package com.unishare.api.modules.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID bookingId;
    private UUID mentorId;
    private UUID packageId;
    private UUID reviewerId;
    private String reviewerName;
    private Integer rating;
    private String comment;
    private Instant createdAt;
    private Instant editedAt;
}
