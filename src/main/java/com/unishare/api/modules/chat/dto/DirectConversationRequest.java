package com.unishare.api.modules.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DirectConversationRequest {

    @NotNull
    private UUID peerUserId;

    /** order | booking | session — metadata khi mở chat từ order/mentoring. */
    private String contextType;

    private UUID contextId;
}
