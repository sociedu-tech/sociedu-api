package com.unishare.api.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Học viên tạo đơn chờ thanh toán (checkout). */
public record OrderCheckoutCreatedEvent(
        UUID orderId,
        UUID buyerId,
        UUID mentorId,
        UUID servicePackageVersionId,
        BigDecimal totalAmount
) implements DomainEvent {}
