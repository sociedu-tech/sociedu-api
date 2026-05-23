package com.unishare.api.modules.notification.service;

import com.unishare.api.modules.notification.dto.NotificationResponse;
import com.unishare.api.modules.notification.dto.RegisterDeviceRequest;
import com.unishare.api.modules.notification.dto.UnregisterDeviceRequest;
import com.unishare.api.modules.notification.dto.UnreadCountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface NotificationService {
    Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable);
    UnreadCountResponse getUnreadCount(UUID userId);
    void markAllRead(UUID userId);
    void markRead(UUID userId, UUID notificationId);
    void registerDevice(UUID userId, RegisterDeviceRequest request);
    void unregisterDevice(UUID userId, UnregisterDeviceRequest request);
    NotificationResponse createNotification(UUID userId, String title, String content, String type, String referenceType, UUID referenceId, Map<String, Object> metadata);
    void sendPushNotificationAsync(UUID notificationId);
}
