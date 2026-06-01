package com.unishare.api.modules.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventEnvelope {
    private String eventType;
    private Instant serverTimestamp;
    private NotificationResponse payload;
}
