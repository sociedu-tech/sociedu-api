package com.unishare.api.modules.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDeviceRequest {
    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Platform is required")
    private String platform;
}
