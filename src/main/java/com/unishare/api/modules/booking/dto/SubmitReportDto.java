package com.unishare.api.modules.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmitReportDto {
    @NotBlank(message = "Nội dung báo cáo không được để trống")
    private String content;
    private String attachmentUrl;
}
