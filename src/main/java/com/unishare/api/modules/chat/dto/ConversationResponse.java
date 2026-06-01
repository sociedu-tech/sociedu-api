package com.unishare.api.modules.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ConversationResponse {
    private UUID id;
    private String type;
    private UUID bookingId;
    private Instant createdAt;
    /** Người còn lại trong hội thoại 1-1 (general). */
    private UUID peerUserId;
    private String peerDisplayName;
    private UUID peerAvatarFileId;
    private String lastMessageContent;
    private Instant lastMessageAt;
    /** Số tin nhắn từ người khác chưa đọc (sau lastReadAt). */
    private Integer unreadCount;
}
