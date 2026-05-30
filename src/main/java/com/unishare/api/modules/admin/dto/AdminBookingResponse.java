package com.unishare.api.modules.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AdminBookingResponse {
    private UUID id;
    private String code;
    private String learnerName;
    private UUID learnerId;
    private String mentorName;
    private UUID mentorId;
    private Instant scheduledAt;
    private Integer durationMin;
    private String status;
    private String packageTitle;
    private BigDecimal amountVnd;
    private Instant createdAt;
}
