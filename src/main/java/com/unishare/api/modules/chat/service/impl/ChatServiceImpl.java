package com.unishare.api.modules.chat.service.impl;

import com.unishare.api.common.constants.ConversationTypes;
import com.unishare.api.common.constants.MessageContextTypes;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.modules.booking.entity.Booking;
import com.unishare.api.modules.booking.entity.BookingSession;
import com.unishare.api.modules.booking.repository.BookingRepository;
import com.unishare.api.modules.booking.repository.BookingSessionRepository;
import com.unishare.api.infrastructure.realtime.RealtimeTopics;
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
import com.unishare.api.modules.auth.repository.UserRepository;
import com.unishare.api.modules.order.entity.Order;
import com.unishare.api.modules.order.repository.OrderRepository;
import com.unishare.api.modules.service.service.CatalogReadService;
import com.unishare.api.modules.user.dto.UserProfileNames;
import com.unishare.api.modules.user.entity.UserProfile;
import com.unishare.api.modules.user.repository.UserProfileRepository;
import com.unishare.api.modules.user.service.UserService;
import com.unishare.api.common.event.ChatMessageReceivedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserProfileRepository userProfileRepository;
    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;
    private final BookingSessionRepository bookingSessionRepository;
    private final CatalogReadService catalogReadService;
    private final ApplicationEventPublisher eventPublisher;

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

        return buildConversationResponse(c, creatorUserId);
    }

    @Override
    @Transactional
    public ConversationResponse findOrCreateDirectConversation(UUID userId, DirectConversationRequest request) {
        UUID peerId = request.getPeerUserId();
        if (peerId == null || peerId.equals(userId)) {
            throw new AppException(ChatErrorCode.INVALID_CHAT_PEER);
        }
        if (!userRepository.existsById(peerId)) {
            throw new AppException(ChatErrorCode.INVALID_CHAT_PEER);
        }

        validateMessageContext(userId, peerId, request.getContextType(), request.getContextId());

        return participantRepository
                .findDirectGeneralConversationId(userId, peerId)
                .or(() -> participantRepository.findDirectGeneralConversationId(peerId, userId))
                .map(conversationRepository::findById)
                .flatMap(opt -> opt.map(conv -> buildConversationResponse(conv, userId)))
                .orElseGet(() -> createGeneralDirectConversation(userId, peerId));
    }

    private ConversationResponse createGeneralDirectConversation(UUID userA, UUID userB) {
        Conversation c = new Conversation();
        c.setType(ConversationTypes.GENERAL);
        c.setBookingId(null);
        c = conversationRepository.save(c);

        UUID cid = c.getId();
        for (UUID uid : List.of(userA, userB)) {
            ConversationParticipant p = new ConversationParticipant();
            p.setId(new ConversationParticipantId(cid, uid));
            participantRepository.save(p);
        }
        return buildConversationResponse(c, userA);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> listMyConversations(UUID userId, Pageable pageable) {
        Page<UUID> idPage = isAdmin(userId)
                ? participantRepository.findAllConversationIdsOrderByRecentActivity(pageable)
                : participantRepository.findConversationIdsForUserOrderByRecentActivity(userId, pageable);
        List<UUID> ids = idPage.getContent();
        Map<UUID, Conversation> byId = conversationRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Conversation::getId, Function.identity()));
        Map<UUID, ChatMessage> lastMessages = loadLastMessagesByConversationIds(ids);
        Map<UUID, Integer> unreadByConversationId = loadUnreadCountsByConversationIds(ids, userId);
        Set<UUID> peerIds = collectPeerIds(ids, userId);
        Map<UUID, UserProfileNames> namesByUserId = userService.getProfileNamesByUserIds(peerIds);
        Map<UUID, UserProfile> profilesByUserId = loadProfilesByUserIds(peerIds);
        List<ConversationResponse> items = dedupeDirectConversationsByPeer(ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(c -> enrichConversationResponse(c, userId, lastMessages, unreadByConversationId, namesByUserId, profilesByUserId))
                .toList());
        return PageResponse.<ConversationResponse>builder()
                .items(items)
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .total(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessages(UUID userId, UUID conversationId) {
        assertParticipant(conversationId, userId);
        List<ChatMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        Map<UUID, UserProfileNames> namesByUserId = loadSenderNames(messages);
        return messages.stream()
                .map(m -> toMessageResponse(m, namesByUserId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Page<ChatMessageResponse> listMessages(UUID userId, UUID conversationId, Pageable pageable) {
        assertParticipant(conversationId, userId);
        Page<ChatMessage> messagePage = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        Map<UUID, UserProfileNames> namesByUserId = loadSenderNames(messagePage.getContent());
        Page<ChatMessageResponse> page = messagePage.map(m -> toMessageResponse(m, namesByUserId));
        if (pageable.getPageNumber() == 0) {
            markConversationRead(userId, conversationId);
        }
        return page;
    }

    @Override
    @Transactional
    public void markConversationRead(UUID userId, UUID conversationId) {
        assertParticipant(conversationId, userId);
        participantRepository.findById_ConversationIdAndId_UserId(conversationId, userId)
                .ifPresent(participant -> {
                    Instant readAt = Instant.now();
                    ChatMessage lastMessage = loadLastMessagesByConversationIds(List.of(conversationId))
                            .get(conversationId);
                    if (lastMessage != null && lastMessage.getCreatedAt() != null
                            && lastMessage.getCreatedAt().isAfter(readAt)) {
                        readAt = lastMessage.getCreatedAt();
                    }
                    participant.setLastReadAt(readAt);
                    participantRepository.save(participant);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID userId, UUID conversationId) {
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ChatErrorCode.CONVERSATION_NOT_FOUND));
        if (!participantRepository.isParticipant(conversationId, userId) && !isAdmin(userId)) {
            throw new AppException(ChatErrorCode.CHAT_ACCESS_DENIED);
        }
        return buildConversationResponse(c, userId);
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(UUID userId, UUID conversationId, SendMessageRequest request) {
        if (isAdmin(userId)) {
            throw new AppException(ChatErrorCode.CHAT_ACCESS_DENIED);
        }
        assertParticipant(conversationId, userId);

        UUID peerId = findDirectPeerId(conversationId, userId).orElse(null);
        if (peerId != null) {
            validateMessageContext(userId, peerId, request.getContextType(), request.getContextId());
        }

        ChatMessage m = new ChatMessage();
        m.setConversationId(conversationId);
        m.setSenderId(userId);
        m.setContent(request.getContent());
        m.setType(request.getType() != null ? request.getType() : "text");
        if (request.getContextType() != null && !request.getContextType().isBlank()) {
            m.setContextType(normalizeContextType(request.getContextType()));
            m.setContextId(request.getContextId());
        }
        m = messageRepository.save(m);

        if (request.getAttachmentFileIds() != null) {
            for (UUID fid : request.getAttachmentFileIds()) {
                MessageAttachment a = new MessageAttachment();
                a.setMessageId(m.getId());
                a.setFileId(fid);
                attachmentRepository.save(a);
            }
        }
        ChatMessageResponse response = toMessageResponse(m, loadSenderNames(List.of(m)));
        ChatEventEnvelope<ChatMessageResponse> envelope = ChatEventEnvelope.<ChatMessageResponse>builder()
                .eventType("NEW_MESSAGE")
                .conversationId(conversationId.toString())
                .serverTimestamp(Instant.now())
                .payload(response)
                .build();
        messagingTemplate.convertAndSend(RealtimeTopics.conversation(conversationId), envelope);

        // Publish domain event for notification system
        if (peerId != null) {
            String senderName = "Người dùng";
            try {
                var names = userService.getProfileNamesByUserIds(java.util.Set.of(userId));
                var profileNames = names.get(userId);
                if (profileNames != null) {
                    String display = profileNames.toDisplayName();
                    if (display != null && !display.isBlank()) {
                        senderName = display;
                    }
                }
            } catch (Exception ignored) {}
            String preview = request.getContent();
            if (preview != null && preview.length() > 100) {
                preview = preview.substring(0, 100) + "…";
            }
            eventPublisher.publishEvent(new ChatMessageReceivedEvent(
                    conversationId, userId, peerId, senderName, preview != null ? preview : ""));
        }

        return response;
    }

    private java.util.Optional<UUID> findDirectPeerId(UUID conversationId, UUID userId) {
        List<UUID> userIds = participantRepository.findUserIdsByConversationId(conversationId);
        if (userIds.size() != 2) {
            return java.util.Optional.empty();
        }
        return userIds.stream().filter(id -> !id.equals(userId)).findFirst();
    }

    private void validateMessageContext(UUID userId, UUID peerId, String contextType, UUID contextId) {
        if (contextType == null || contextType.isBlank()) {
            return;
        }
        String type = normalizeContextType(contextType);
        if (MessageContextTypes.GENERAL.equals(type)) {
            return;
        }
        if (contextId == null) {
            throw new AppException(ChatErrorCode.INVALID_MESSAGE_CONTEXT);
        }

        switch (type) {
            case MessageContextTypes.ORDER -> assertOrderContext(userId, peerId, contextId);
            case MessageContextTypes.BOOKING -> assertBookingContext(userId, peerId, contextId);
            case MessageContextTypes.SESSION -> assertSessionContext(userId, peerId, contextId);
            default -> throw new AppException(ChatErrorCode.INVALID_MESSAGE_CONTEXT);
        }
    }

    private void assertOrderContext(UUID userId, UUID peerId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ChatErrorCode.INVALID_MESSAGE_CONTEXT));

        bookingRepository.findByOrderId(orderId).ifPresentOrElse(
                booking -> assertBookingParticipants(userId, peerId, booking),
                () -> assertOrderParticipants(userId, peerId, order));
    }

    private void assertOrderParticipants(UUID userId, UUID peerId, Order order) {
        UUID mentorId;
        try {
            mentorId = catalogReadService.resolvePurchaseContext(order.getServiceId()).mentorId();
        } catch (AppException e) {
            throw new AppException(ChatErrorCode.INVALID_MESSAGE_CONTEXT);
        }
        boolean asBuyer = userId.equals(order.getBuyerId()) && peerId.equals(mentorId);
        boolean asMentor = userId.equals(mentorId) && peerId.equals(order.getBuyerId());
        if (!asBuyer && !asMentor) {
            throw new AppException(ChatErrorCode.CHAT_ACCESS_DENIED);
        }
    }

    private void assertBookingContext(UUID userId, UUID peerId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ChatErrorCode.INVALID_MESSAGE_CONTEXT));
        assertBookingParticipants(userId, peerId, booking);
    }

    private void assertSessionContext(UUID userId, UUID peerId, UUID sessionId) {
        BookingSession session = bookingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ChatErrorCode.INVALID_MESSAGE_CONTEXT));
        Booking booking = bookingRepository.findById(session.getBookingId())
                .orElseThrow(() -> new AppException(ChatErrorCode.INVALID_MESSAGE_CONTEXT));
        assertBookingParticipants(userId, peerId, booking);
    }

    private void assertBookingParticipants(UUID userId, UUID peerId, Booking booking) {
        boolean asBuyer = userId.equals(booking.getBuyerId()) && peerId.equals(booking.getMentorId());
        boolean asMentor = userId.equals(booking.getMentorId()) && peerId.equals(booking.getBuyerId());
        if (!asBuyer && !asMentor) {
            throw new AppException(ChatErrorCode.CHAT_ACCESS_DENIED);
        }
    }

    private String normalizeContextType(String contextType) {
        return contextType.trim().toLowerCase();
    }

    private boolean isAdmin(UUID userId) {
        return userRepository.findByIdWithRoles(userId)
                .map(u -> u.getUserRoles().stream().anyMatch(ur -> "ADMIN".equalsIgnoreCase(ur.getRole().getName())))
                .orElse(false);
    }

    private void assertParticipant(UUID conversationId, UUID userId) {
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ChatErrorCode.CONVERSATION_NOT_FOUND));
        if (!participantRepository.isParticipant(conversationId, userId) && !isAdmin(userId)) {
            throw new AppException(ChatErrorCode.CHAT_ACCESS_DENIED);
        }
    }

    private ConversationResponse buildConversationResponse(Conversation c, UUID viewerUserId) {
        List<UUID> ids = List.of(c.getId());
        Map<UUID, ChatMessage> lastMessages = loadLastMessagesByConversationIds(ids);
        Map<UUID, Integer> unreadByConversationId = loadUnreadCountsByConversationIds(ids, viewerUserId);
        Set<UUID> peerIds = collectPeerIds(ids, viewerUserId);
        Map<UUID, UserProfileNames> namesByUserId = userService.getProfileNamesByUserIds(peerIds);
        Map<UUID, UserProfile> profilesByUserId = loadProfilesByUserIds(peerIds);
        return enrichConversationResponse(c, viewerUserId, lastMessages, unreadByConversationId, namesByUserId, profilesByUserId);
    }

    private ConversationResponse enrichConversationResponse(
            Conversation c,
            UUID viewerUserId,
            Map<UUID, ChatMessage> lastMessageByConversationId,
            Map<UUID, Integer> unreadByConversationId,
            Map<UUID, UserProfileNames> namesByUserId,
            Map<UUID, UserProfile> profilesByUserId) {
        ChatMessage lastMessage = lastMessageByConversationId.get(c.getId());
        List<UUID> participantIds = participantRepository.findUserIdsByConversationId(c.getId());
        boolean isViewerParticipant = participantIds.contains(viewerUserId);

        UUID peerId = null;
        String customDisplayName = null;

        if (isViewerParticipant) {
            peerId = participantIds.stream().filter(id -> !id.equals(viewerUserId)).findFirst().orElse(null);
        } else {
            List<String> names = new ArrayList<>();
            for (UUID id : participantIds) {
                UserProfileNames n = namesByUserId.get(id);
                String disp = formatDisplayName(id, n);
                if (disp != null && !disp.isBlank()) {
                    names.add(disp);
                }
            }
            if (!names.isEmpty()) {
                customDisplayName = String.join(" ↔ ", names);
            }
            peerId = participantIds.isEmpty() ? null : participantIds.get(0);
        }

        UserProfileNames peerNames = peerId != null ? namesByUserId.get(peerId) : null;
        UserProfile peerProfile = peerId != null ? profilesByUserId.get(peerId) : null;

        return ConversationResponse.builder()
                .id(c.getId())
                .type(c.getType())
                .bookingId(c.getBookingId())
                .createdAt(c.getCreatedAt())
                .peerUserId(peerId)
                .peerDisplayName(customDisplayName != null ? customDisplayName : formatDisplayName(peerId, peerNames))
                .peerAvatarFileId(peerProfile != null ? peerProfile.getAvatarFileId() : null)
                .lastMessageContent(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageAt(lastMessage != null ? lastMessage.getCreatedAt() : null)
                .unreadCount(unreadByConversationId.getOrDefault(c.getId(), 0))
                .build();
    }

    private UUID resolvePeerId(UUID conversationId, UUID viewerUserId) {
        List<UUID> userIds = participantRepository.findUserIdsByConversationId(conversationId);
        if (userIds.size() != 2) {
            return null;
        }
        return userIds.stream().filter(id -> !id.equals(viewerUserId)).findFirst().orElse(null);
    }

    private String formatDisplayName(UUID userId, UserProfileNames names) {
        if (names != null) {
            String display = names.toDisplayName();
            if (display != null && !display.isBlank()) {
                return display;
            }
        }
        if (userId == null) {
            return null;
        }
        String id = userId.toString().replace("-", "");
        String shortId = id.length() <= 8 ? id : id.substring(0, 8);
        return "Người dùng #" + shortId;
    }

    private Map<UUID, Integer> loadUnreadCountsByConversationIds(Collection<UUID> conversationIds, UUID userId) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        for (UUID conversationId : conversationIds) {
            counts.put(conversationId, 0);
        }
        for (Object[] row : messageRepository.countUnreadByConversationIdsForUser(conversationIds, userId)) {
            UUID conversationId = (UUID) row[0];
            Number count = (Number) row[1];
            counts.put(conversationId, count != null ? count.intValue() : 0);
        }
        return counts;
    }

    private Map<UUID, ChatMessage> loadLastMessagesByConversationIds(Collection<UUID> conversationIds) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Map.of();
        }
        return messageRepository.findLatestByConversationIds(conversationIds).stream()
                .collect(Collectors.toMap(ChatMessage::getConversationId, Function.identity(), (a, b) -> a));
    }

    private Map<UUID, UserProfile> loadProfilesByUserIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userProfileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity()));
    }

    private Set<UUID> collectPeerIds(Collection<UUID> conversationIds, UUID viewerUserId) {
        Set<UUID> peerIds = new LinkedHashSet<>();
        for (UUID conversationId : conversationIds) {
            List<UUID> uids = participantRepository.findUserIdsByConversationId(conversationId);
            peerIds.addAll(uids);
        }
        peerIds.remove(viewerUserId);
        return peerIds;
    }

    private List<ConversationResponse> dedupeDirectConversationsByPeer(List<ConversationResponse> items) {
        Map<UUID, ConversationResponse> preferredByPeer = new LinkedHashMap<>();
        for (ConversationResponse item : items) {
            UUID peerId = item.getPeerUserId();
            if (peerId == null) {
                continue;
            }
            preferredByPeer.merge(peerId, item, this::preferConversation);
        }

        Set<UUID> emittedPeers = new HashSet<>();
        List<ConversationResponse> result = new ArrayList<>();
        for (ConversationResponse item : items) {
            UUID peerId = item.getPeerUserId();
            if (peerId == null) {
                result.add(item);
                continue;
            }
            if (emittedPeers.add(peerId)) {
                result.add(preferredByPeer.get(peerId));
            }
        }
        return result;
    }

    private ConversationResponse preferConversation(ConversationResponse a, ConversationResponse b) {
        boolean aGeneral = ConversationTypes.GENERAL.equals(a.getType());
        boolean bGeneral = ConversationTypes.GENERAL.equals(b.getType());
        if (aGeneral && !bGeneral) {
            return a;
        }
        if (bGeneral && !aGeneral) {
            return b;
        }
        Instant aAt = a.getLastMessageAt() != null ? a.getLastMessageAt() : a.getCreatedAt();
        Instant bAt = b.getLastMessageAt() != null ? b.getLastMessageAt() : b.getCreatedAt();
        if (aAt == null) {
            return b;
        }
        if (bAt == null) {
            return a;
        }
        return aAt.isAfter(bAt) ? a : b;
    }

    private Map<UUID, UserProfileNames> loadSenderNames(Collection<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return Map.of();
        }
        Set<UUID> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (senderIds.isEmpty()) {
            return Map.of();
        }
        return userService.getProfileNamesByUserIds(senderIds);
    }

    private ChatMessageResponse toMessageResponse(ChatMessage m, Map<UUID, UserProfileNames> namesByUserId) {
        List<UUID> fileIds = attachmentRepository.findByMessageId(m.getId()).stream()
                .map(MessageAttachment::getFileId)
                .collect(Collectors.toList());
        return ChatMessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .senderDisplayName(formatDisplayName(m.getSenderId(), namesByUserId.get(m.getSenderId())))
                .content(m.getContent())
                .type(m.getType())
                .edited(m.getEdited())
                .createdAt(m.getCreatedAt())
                .attachmentFileIds(fileIds.isEmpty() ? null : fileIds)
                .contextType(m.getContextType())
                .contextId(m.getContextId())
                .build();
    }
}
