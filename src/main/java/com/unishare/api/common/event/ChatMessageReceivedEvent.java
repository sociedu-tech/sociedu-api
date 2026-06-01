package com.unishare.api.common.event;

import java.util.UUID;

public record ChatMessageReceivedEvent(
    UUID conversationId,
    UUID senderId,
    UUID recipientId,
    String senderDisplayName,
    String messagePreview
) implements DomainEvent {}
