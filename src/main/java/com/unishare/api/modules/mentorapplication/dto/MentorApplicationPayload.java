package com.unishare.api.modules.mentorapplication.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class MentorApplicationPayload {

    @NotBlank
    private String headline;

    @NotBlank
    private String bio;

    @NotEmpty
    private List<String> expertise;

    @NotNull
    @Min(0)
    private Integer yearsOfExperience;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal hourlyRate;

    private UUID cvFileId;
    private String cvUrl;
    private List<String> portfolioUrls;
    private List<MentorApplicationCertificate> certificates;
}
