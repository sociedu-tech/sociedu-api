package com.unishare.api.modules.notification.dispatch;

import com.unishare.api.modules.notification.realtime.NotificationRealtimePublisher;
import com.unishare.api.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private final NotificationService notificationService;
    private final NotificationRealtimePublisher notificationRealtimePublisher;

    public void deliverAll(List<NotificationDispatchCommand> commands) {
        for (NotificationDispatchCommand cmd : commands) {
            var created = notificationService.createNotification(
                    cmd.userId(),
                    cmd.title(),
                    cmd.content(),
                    cmd.type(),
                    cmd.referenceType(),
                    cmd.referenceId(),
                    cmd.metadata());
            notificationService.sendPushNotificationAsync(created.getId());
            notificationRealtimePublisher.publishToUser(cmd.userId(), created);
        }
    }
}
