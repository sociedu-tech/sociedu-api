package com.unishare.api.modules.notification.dispatch;

import java.util.Map;
import java.util.UUID;

/** Một thông báo in-app + push cho một user. */
public record NotificationDispatchCommand(
        UUID userId,
        String title,
        String content,
        String type,
        String referenceType,
        UUID referenceId,
        Map<String, Object> metadata
) {}
