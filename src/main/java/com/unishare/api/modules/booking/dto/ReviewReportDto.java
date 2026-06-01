package com.unishare.api.modules.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewReportDto {
    @NotBlank(message = "Trạng thái không được để trống")
    private String status; // APPROVED or REJECTED
    private String feedback;
}
