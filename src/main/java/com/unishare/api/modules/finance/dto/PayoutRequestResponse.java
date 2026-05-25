package com.unishare.api.modules.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PayoutRequestResponse {
    private UUID id;
    private UUID mentorId;
    private BigDecimal grossAmount;
    private BigDecimal platformFeeRate;
    private BigDecimal netAmount;
    private String status;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String rejectReason;
    private String failureReason;
    private String transactionReference;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant processedAt;

    public static String maskAccountNumber(String accNum) {
        if (accNum == null) {
            return null;
        }
        if (accNum.length() <= 4) {
            return "****";
        }
        return "*******" + accNum.substring(accNum.length() - 4);
    }
}
