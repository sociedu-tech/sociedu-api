package com.unishare.api.infrastructure.event;

import com.unishare.api.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Phát event trong JVM (Spring) + tùy chọn ra integration bus.
 */
@Component
@RequiredArgsConstructor
public class DomainEventPublisherImpl implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final IntegrationEventPublisher integrationEventPublisher;

    @Override
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
        integrationEventPublisher.publish(event);
    }
}
