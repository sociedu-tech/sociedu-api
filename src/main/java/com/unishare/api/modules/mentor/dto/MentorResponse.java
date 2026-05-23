package com.unishare.api.modules.mentor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class MentorResponse {

    private UUID userId;
    /** Tên hiển thị từ {@code user_profiles} (khi có). */
    private String displayName;
    private String headline;
    private String expertise;
    private BigDecimal basePrice;
    private Double ratingAvg;
    private Integer sessionsCompleted;
    private String verificationStatus;
}
