package com.unishare.api.modules.mentorapplication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminRejectMentorApplicationRequest {

    @NotBlank
    private String reason;

    private String note;
}
