package com.unishare.api.modules.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePayoutRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "50000.0", message = "Minimum payout amount is 50,000 VND")
    private BigDecimal amount;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Account holder is required")
    private String accountHolder;
}
