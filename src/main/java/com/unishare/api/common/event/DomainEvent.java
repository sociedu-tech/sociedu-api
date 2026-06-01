package com.unishare.api.common.event;

import com.unishare.api.infrastructure.event.DomainEventPublisher;

import java.time.Instant;

/**
 * Kiểu chung cho mọi event phát qua {@link DomainEventPublisher}.
 * Record/event mới trong module chỉ cần {@code implements DomainEvent} để type-safe và dễ mở rộng
 * (Kafka, outbox, logging) tại một implementation.
 */
public interface DomainEvent {

    /** Tên ổn định cho log / message bus (mặc định = simple class name). */
    default String eventType() {
        return getClass().getSimpleName();
    }

    default Instant occurredAt() {
        return Instant.now();
    }
}
