package com.unishare.api.modules.mentorapplication.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MentorApplicationResponse {
    private UUID id;
    private UUID userId;
    private String status;
    private String headline;
    private String bio;
    private List<String> expertise;
    private Integer yearsOfExperience;
    private BigDecimal hourlyRate;
    private UUID cvFileId;
    private String cvUrl;
    private List<String> portfolioUrls;
    private List<MentorApplicationCertificate> certificates;
    private String reason;
    private String note;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private Integer resubmitCount;
    private Instant createdAt;
    private Instant updatedAt;
    private MentorApplicationApplicantDto applicant;
}
