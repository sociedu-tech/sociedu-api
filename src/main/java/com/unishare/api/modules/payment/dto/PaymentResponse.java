package com.unishare.api.modules.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class PaymentResponse {
    private UUID id;
    private UUID orderId;
    private String provider;
    /** Giá trị khớp cột provider_transaction_id (VNPay vnp_TxnRef). */
    private String transactionRef;
    private BigDecimal amount;
    private String status;
    private Instant createdAt;
    private String paymentUrl;
}
