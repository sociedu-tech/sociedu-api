package com.unishare.api.modules.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelBookingRequest {
    @NotBlank(message = "Cancel reason is required")
    private String reason;
}
