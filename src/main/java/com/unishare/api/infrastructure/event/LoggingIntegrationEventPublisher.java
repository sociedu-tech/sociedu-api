package com.unishare.api.infrastructure.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishare.api.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mặc định: ghi log payload (dev). Bật Kafka: {@code app.events.integration.type=kafka}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.events.integration.type", havingValue = "logging", matchIfMissing = true)
public class LoggingIntegrationEventPublisher implements IntegrationEventPublisher {

    private final ObjectMapper objectMapper;

    @Override
    public void publish(DomainEvent event) {
        if (!log.isDebugEnabled()) {
            return;
        }
        try {
            log.debug("[IntegrationEvent] type={} payload={}", event.eventType(), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            log.debug("[IntegrationEvent] type={} payload={}", event.eventType(), event);
        }
    }
}
