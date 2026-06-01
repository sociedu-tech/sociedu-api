package com.unishare.api.infrastructure.event;

import com.unishare.api.common.event.DomainEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.events.integration.type", havingValue = "none")
public class NoOpIntegrationEventPublisher implements IntegrationEventPublisher {

    @Override
    public void publish(DomainEvent event) {
        // intentionally empty
    }
}
