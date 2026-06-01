package com.unishare.api.infrastructure.event.kafka;

import com.unishare.api.common.event.DomainEvent;
import com.unishare.api.infrastructure.event.IntegrationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stub Kafka — cắm {@code KafkaTemplate} khi có topic naming convention.
 * Topic gợi ý: {@code unishare.domain-events} keyed by {@code event.eventType()}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.events.integration.type", havingValue = "kafka")
public class KafkaIntegrationEventPublisher implements IntegrationEventPublisher {

    @Override
    public void publish(DomainEvent event) {
        log.info("[Kafka stub] would publish type={} at={}", event.eventType(), event.occurredAt());
    }
}
