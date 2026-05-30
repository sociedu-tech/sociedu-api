package com.unishare.api.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminResolveModerationRequest {

    /** FE: open | in_review | resolved | dismissed */
    @NotBlank
    private String status;

    private String resolutionNote;
}
