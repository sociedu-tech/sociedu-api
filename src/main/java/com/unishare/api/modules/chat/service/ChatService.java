package com.unishare.api.modules.chat.service;

import com.unishare.api.modules.chat.dto.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    ConversationResponse createConversation(UUID creatorUserId, CreateConversationRequest request);

    List<ConversationResponse> listMyConversations(UUID userId);

    ConversationResponse getConversation(UUID userId, UUID conversationId);

    List<ChatMessageResponse> listMessages(UUID userId, UUID conversationId);

    Page<ChatMessageResponse> listMessages(UUID userId, UUID conversationId, Pageable pageable);

    ChatMessageResponse sendMessage(UUID userId, UUID conversationId, SendMessageRequest request);
}
