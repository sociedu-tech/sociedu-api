package com.unishare.api.modules.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConfirmSessionCompletionRequest {

    /** true = đồng ý buổi học đã hoàn thành; false = từ chối / chưa hoàn thành */
    @NotNull
    private Boolean completed;
}
