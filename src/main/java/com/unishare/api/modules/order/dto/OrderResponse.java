package com.unishare.api.modules.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class OrderResponse {
    private UUID id;
    private UUID buyerId;
    /** service_package_versions.id */
    private UUID serviceId;
    private String status;
    private BigDecimal totalAmount;
    private Instant paidAt;
    private Instant createdAt;

    private String paymentUrl;
    /** true khi thanh toán được mock (dev), client redirect thẳng tới payment-result. */
    private Boolean mockPayment;
}
