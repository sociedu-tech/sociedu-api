package com.unishare.api.modules.mentorapplication.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class MentorApplicationApplicantDto {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private Instant createdAt;
}
