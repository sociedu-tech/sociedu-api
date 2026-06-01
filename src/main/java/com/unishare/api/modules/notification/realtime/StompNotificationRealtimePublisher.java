package com.unishare.api.modules.notification.realtime;

import com.unishare.api.modules.notification.dto.NotificationEventEnvelope;
import com.unishare.api.modules.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StompNotificationRealtimePublisher implements NotificationRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publishToUser(UUID userId, NotificationResponse notification) {
        NotificationEventEnvelope envelope = NotificationEventEnvelope.builder()
                .eventType("NEW_NOTIFICATION")
                .serverTimestamp(Instant.now())
                .payload(notification)
                .build();
        messagingTemplate.convertAndSend("/topic/users/" + userId + "/notifications", envelope);
    }
}
