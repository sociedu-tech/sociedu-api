package com.unishare.api.modules.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateGoogleMeetSessionRequest {

    @NotNull
    private Instant scheduledAt;

    /** Nếu null, mặc định = scheduledAt + 60 phút. */
    private Instant scheduledAtEnd;

    /** Tuỳ chọn — mặc định lấy tiêu đề buổi học. */
    private String title;

    private String description;
}
