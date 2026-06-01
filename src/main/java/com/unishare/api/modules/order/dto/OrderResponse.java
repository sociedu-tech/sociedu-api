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
    /** Tên gói dịch vụ (enrich). */
    private String packageName;
    /** Mentor sở hữu gói (enrich). */
    private UUID mentorId;
    /** Nhãn hiển thị học viên (enrich). */
    private String buyerLabel;
    private String status;
    private BigDecimal totalAmount;
    private Instant paidAt;
    private Instant createdAt;
    /** Hết hạn chờ thanh toán — chỉ có khi status = pending_payment. */
    private Instant paymentExpiresAt;
    /** Có thể mở / mở lại cổng thanh toán. */
    private Boolean canPay;

    private String paymentUrl;
}
