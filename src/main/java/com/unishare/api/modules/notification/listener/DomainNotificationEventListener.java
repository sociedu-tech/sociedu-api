package com.unishare.api.modules.notification.listener;

import com.unishare.api.common.event.DomainEvent;
import com.unishare.api.modules.notification.dispatch.DomainNotificationHandler;
import com.unishare.api.modules.notification.dispatch.NotificationDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Một listener tập trung: mọi domain event nghiệp vụ → notification in-app + push.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainNotificationEventListener {

    private final DomainNotificationHandler notificationHandler;
    private final NotificationDeliveryService deliveryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDomainEvent(DomainEvent event) {
        if (!notificationHandler.supports(event)) {
            return;
        }
        try {
            var commands = notificationHandler.resolve(event);
            if (!commands.isEmpty()) {
                deliveryService.deliverAll(commands);
                log.info("Delivered {} notification(s) for {}", commands.size(), event.eventType());
            }
        } catch (Exception e) {
            log.error("Failed to deliver notifications for event type={}", event.eventType(), e);
        }
    }
}
