package com.unishare.api.modules.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ChatMessageResponse {
    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    /** Tên hiển thị người gửi (dùng cho admin xem hội thoại giữa 2 user). */
    private String senderDisplayName;
    private String content;
    private String type;
    private Boolean edited;
    private Instant createdAt;
    private List<UUID> attachmentFileIds;
    private String contextType;
    private UUID contextId;
}
