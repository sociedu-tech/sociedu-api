package com.unishare.api.modules.notification.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private String title;
    private String content;
    private String type;
    private String referenceType;
    private UUID referenceId;
    private Map<String, Object> metadata;
    private Boolean isRead;
    private Instant readAt;
    private Instant createdAt;
}
