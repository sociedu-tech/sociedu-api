package com.unishare.api.modules.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEventEnvelope<T> {
    private String eventType;
    private String conversationId;
    private Instant serverTimestamp;
    private T payload;
}
