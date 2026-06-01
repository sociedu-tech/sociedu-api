package com.unishare.api.modules.notification.dispatch;

import com.unishare.api.modules.notification.realtime.NotificationRealtimePublisher;
import com.unishare.api.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private final NotificationService notificationService;
    private final NotificationRealtimePublisher notificationRealtimePublisher;

    public void deliverAll(List<NotificationDispatchCommand> commands) {
        for (NotificationDispatchCommand cmd : commands) {
            try {
                var created = notificationService.createNotification(
                        cmd.userId(),
                        cmd.title(),
                        cmd.content(),
                        cmd.type(),
                        cmd.referenceType(),
                        cmd.referenceId(),
                        cmd.metadata());
                notificationRealtimePublisher.publishToUser(cmd.userId(), created);
            } catch (Exception e) {
                log.error(
                        "Failed to deliver notification to userId={} type={} ref={}",
                        cmd.userId(),
                        cmd.type(),
                        cmd.referenceId(),
                        e);
            }
        }
    }
}
