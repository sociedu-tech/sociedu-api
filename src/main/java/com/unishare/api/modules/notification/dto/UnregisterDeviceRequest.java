package com.unishare.api.modules.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UnregisterDeviceRequest {
    @NotBlank(message = "Token is required")
    private String token;
}
