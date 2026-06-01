package com.unishare.api.common.event;

import java.util.UUID;

/**
 * Đơn đã thanh toán thành công — lắng nghe: booking, notification, email, integration bus.
 */
public record OrderPaidEvent(UUID orderId, UUID buyerId) implements DomainEvent {}
