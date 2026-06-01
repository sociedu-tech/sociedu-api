package com.unishare.api.infrastructure.event;

import com.unishare.api.common.event.DomainEvent;

/**
 * Cổng xuất event ra hạ tầng ngoài (Kafka, RabbitMQ, …).
 * Tách khỏi {@link DomainEventPublisher} để in-process listener (notification, mail, booking)
 * không phụ thuộc message bus.
 */
public interface IntegrationEventPublisher {

    void publish(DomainEvent event);
}
