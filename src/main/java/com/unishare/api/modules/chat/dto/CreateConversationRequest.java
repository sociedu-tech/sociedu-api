package com.unishare.api.modules.chat.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateConversationRequest {

    @NotNull
    private String type;

    /** Optional — chỉ cần thiết khi type = booking */
    private UUID bookingId;

    @NotEmpty
    private List<UUID> participantUserIds;
}

