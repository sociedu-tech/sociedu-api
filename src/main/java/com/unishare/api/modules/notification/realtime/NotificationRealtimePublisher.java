package com.unishare.api.modules.notification.realtime;

import com.unishare.api.modules.notification.dto.NotificationResponse;

import java.util.UUID;

/** Đẩy notification realtime tới web/mobile (STOMP; sau này có thể thêm SSE). */
public interface NotificationRealtimePublisher {

    void publishToUser(UUID userId, NotificationResponse notification);
}
