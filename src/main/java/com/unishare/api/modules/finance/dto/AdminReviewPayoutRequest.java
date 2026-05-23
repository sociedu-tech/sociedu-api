package com.unishare.api.modules.finance.dto;

import lombok.Data;

@Data
public class AdminReviewPayoutRequest {
    private String rejectReason;
    private String failureReason;
    private String transactionReference;
}
