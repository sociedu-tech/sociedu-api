package com.unishare.api.modules.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CreateReportRequestDto {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    private String description;
    private Instant dueDate;
    private UUID sessionId;
}
