package com.unishare.api.modules.chat.dto;

import com.unishare.api.common.constants.MessageTypes;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SendMessageRequest {
    private String content;
    private String type = MessageTypes.TEXT;
    private List<UUID> attachmentFileIds;
    /** order | booking | session — gắn ngữ cảnh lên tin nhắn, không tạo hội thoại riêng. */
    private String contextType;
    private UUID contextId;
}
