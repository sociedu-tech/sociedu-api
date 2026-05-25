package com.unishare.api.modules.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RevenueSummaryResponse {
    private BigDecimal totalEarned;
    private BigDecimal totalWithdrawn;
    private BigDecimal lockedBalance;
    private BigDecimal availableBalance;
}
