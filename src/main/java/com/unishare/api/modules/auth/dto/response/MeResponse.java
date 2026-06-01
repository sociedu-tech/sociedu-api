package com.unishare.api.modules.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Data returned for GET /api/v1/auth/me - single source of truth for FE session. */
@Data
@Builder
public class MeResponse {
    private UUID userId;
    private String email;
    private Boolean emailVerified;
    private String status;
    private String firstName;
    private String lastName;
    private String fullName;
    private String headline;
    private String avatarUrl;
    private List<String> roles;
    private Instant createdAt;
}
