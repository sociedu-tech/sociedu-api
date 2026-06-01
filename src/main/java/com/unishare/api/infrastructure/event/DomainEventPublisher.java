package com.unishare.api.infrastructure.event;

import com.unishare.api.common.event.DomainEvent;

/**
 * Cổng phát domain event — toàn bộ nơi cần bắn event nên inject interface này thay cho Spring
 * {@link org.springframework.context.ApplicationEventPublisher} trực tiếp.
 * Implementation: in-process Spring events + {@link IntegrationEventPublisher} (Kafka-ready).
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
