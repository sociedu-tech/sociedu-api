package com.unishare.api.modules.notification.service.impl;

import com.unishare.api.common.dto.AppException;
import com.unishare.api.modules.notification.dto.NotificationResponse;
import com.unishare.api.modules.notification.dto.RegisterDeviceRequest;
import com.unishare.api.modules.notification.dto.UnregisterDeviceRequest;
import com.unishare.api.modules.notification.dto.UnreadCountResponse;
import com.unishare.api.modules.notification.entity.DeviceToken;
import com.unishare.api.modules.notification.entity.Notification;
import com.unishare.api.modules.notification.exception.NotificationErrorCode;
import com.unishare.api.modules.notification.repository.DeviceTokenRepository;
import com.unishare.api.modules.notification.repository.NotificationRepository;
import com.unishare.api.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final List<String> INBOX_EXCLUDED_TYPES = List.of("CHAT");

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository
                .findActionInbox(userId, INBOX_EXCLUDED_TYPES, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        long count = notificationRepository.countActionUnread(userId, INBOX_EXCLUDED_TYPES);
        return new UnreadCountResponse(count);
    }

    @Override
    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllAsRead(userId, Instant.now());
    }

    @Override
    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(NotificationErrorCode.NOTIFICATION_NOT_FOUND, "Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new AppException(NotificationErrorCode.NOTIFICATION_ACCESS_DENIED, "Access denied to this notification");
        }

        if (Boolean.FALSE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void registerDevice(UUID userId, RegisterDeviceRequest request) {
        Optional<DeviceToken> existing = deviceTokenRepository.findByToken(request.getToken());
        if (existing.isPresent()) {
            DeviceToken token = existing.get();
            token.setUserId(userId);
            token.setPlatform(request.getPlatform());
            token.setLastSeenAt(Instant.now());
            deviceTokenRepository.save(token);
        } else {
            DeviceToken token = new DeviceToken();
            token.setUserId(userId);
            token.setToken(request.getToken());
            token.setPlatform(request.getPlatform());
            token.setLastSeenAt(Instant.now());
            deviceTokenRepository.save(token);
        }
    }

    @Override
    @Transactional
    public void unregisterDevice(UUID userId, UnregisterDeviceRequest request) {
        deviceTokenRepository.deleteByUserIdAndToken(userId, request.getToken());
    }

    @Override
    @Transactional
    public NotificationResponse createNotification(UUID userId, String title, String content, String type, String referenceType, UUID referenceId, Map<String, Object> metadata) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setMetadata(metadata);
        notification = notificationRepository.save(notification);
        return mapToResponse(notification);
    }

    @Override
    @Async
    @Transactional
    public void sendPushNotificationAsync(UUID notificationId) {
        log.info("Starting async push delivery for notification ID: {}", notificationId);
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            log.warn("Notification not found for async push delivery: {}", notificationId);
            return;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(notification.getUserId());
        if (tokens.isEmpty()) {
            log.info("No registered device tokens for user: {}. Skipping push delivery.", notification.getUserId());
            notification.setPushStatus("SENT"); // Mock as sent/no-op
            notificationRepository.save(notification);
            return;
        }

        try {
            // Mocking Firebase FCM call
            for (DeviceToken token : tokens) {
                log.info("Pushing notification to platform: {} using token: {}. Title: {}, Content: {}",
                        token.getPlatform(), token.getToken(), notification.getTitle(), notification.getContent());
            }
            notification.setPushStatus("SENT");
        } catch (Exception e) {
            log.error("FCM mock delivery failed for notification ID: {}", notificationId, e);
            notification.setPushStatus("FAILED");
        }
        notificationRepository.save(notification);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .metadata(notification.getMetadata())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
