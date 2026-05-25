package com.unishare.api.modules.chat.service.impl;

import com.unishare.api.common.dto.AppException;
import com.unishare.api.modules.chat.dto.*;
import com.unishare.api.modules.chat.entity.ChatMessage;
import com.unishare.api.modules.chat.entity.Conversation;
import com.unishare.api.modules.chat.entity.ConversationParticipant;
import com.unishare.api.modules.chat.entity.ConversationParticipantId;
import com.unishare.api.modules.chat.entity.MessageAttachment;
import com.unishare.api.modules.chat.exception.ChatErrorCode;
import com.unishare.api.modules.chat.repository.ChatMessageRepository;
import com.unishare.api.modules.chat.repository.ConversationParticipantRepository;
import com.unishare.api.modules.chat.repository.ConversationRepository;
import com.unishare.api.modules.chat.repository.MessageAttachmentRepository;
import com.unishare.api.modules.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ConversationResponse createConversation(UUID creatorUserId, CreateConversationRequest request) {
        Set<UUID> users = new LinkedHashSet<>(request.getParticipantUserIds());
        users.add(creatorUserId);

        Conversation c = new Conversation();
        c.setType(request.getType());
        c.setBookingId(request.getBookingId());
        c = conversationRepository.save(c);

        UUID cid = c.getId();
        for (UUID uid : users) {
            ConversationParticipant p = new ConversationParticipant();
            p.setId(new ConversationParticipantId(cid, uid));
            participantRepository.save(p);
        }

        return toConvResponse(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> listMyConversations(UUID userId) {
        List<UUID> ids = participantRepository.findConversationIdsByUserId(userId);
        return conversationRepository.findAllById(ids).stream()
                .map(this::toConvResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessages(UUID userId, UUID conversationId) {
        assertParticipant(conversationId, userId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> listMessages(UUID userId, UUID conversationId, Pageable pageable) {
        assertParticipant(conversationId, userId);
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(this::toMessageResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID userId, UUID conversationId) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ChatErrorCode.CONVERSATION_NOT_FOUND));
        if (!participantRepository.isParticipant(conversationId, userId)) {
            throw new AppException(ChatErrorCode.CHAT_ACCESS_DENIED);
        }
        return toConvResponse(c);
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(UUID userId, UUID conversationId, SendMessageRequest request) {
        assertParticipant(conversationId, userId);
        ChatMessage m = new ChatMessage();
        m.setConversationId(conversationId);
        m.setSenderId(userId);
        m.setContent(request.getContent());
        m.setType(request.getType() != null ? request.getType() : "text");
        m = messageRepository.save(m);

        if (request.getAttachmentFileIds() != null) {
            for (UUID fid : request.getAttachmentFileIds()) {
                MessageAttachment a = new MessageAttachment();
                a.setMessageId(m.getId());
                a.setFileId(fid);
                attachmentRepository.save(a);
            }
        }
        ChatMessageResponse response = toMessageResponse(m);
        ChatEventEnvelope<ChatMessageResponse> envelope = ChatEventEnvelope.<ChatMessageResponse>builder()
                .eventType("NEW_MESSAGE")
                .conversationId(conversationId.toString())
                .serverTimestamp(Instant.now())
                .payload(response)
                .build();
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, envelope);
        return response;
    }

    private void assertParticipant(UUID conversationId, UUID userId) {
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ChatErrorCode.CONVERSATION_NOT_FOUND));
        if (!participantRepository.isParticipant(conversationId, userId)) {
            throw new AppException(ChatErrorCode.CHAT_ACCESS_DENIED);
        }
    }

    private ConversationResponse toConvResponse(Conversation c) {
        return ConversationResponse.builder()
                .id(c.getId())
                .type(c.getType())
                .bookingId(c.getBookingId())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage m) {
        List<UUID> fileIds = attachmentRepository.findByMessageId(m.getId()).stream()
                .map(MessageAttachment::getFileId)
                .collect(Collectors.toList());
        return ChatMessageResponse.builder()
                .id(m.getId())
                .senderId(m.getSenderId())
                .content(m.getContent())
                .type(m.getType())
                .edited(m.getEdited())
                .createdAt(m.getCreatedAt())
                .attachmentFileIds(fileIds.isEmpty() ? null : fileIds)
                .build();
    }
}
