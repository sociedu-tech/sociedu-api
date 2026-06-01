package com.unishare.api.infrastructure.security;

import com.unishare.api.infrastructure.realtime.RealtimeTopics;
import com.unishare.api.modules.chat.repository.ConversationParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final ConversationParticipantRepository participantRepository;

    private static final Pattern CONVERSATION_TOPIC_PATTERN = RealtimeTopics.CONVERSATION_TOPIC;
    private static final Pattern USER_NOTIFICATION_TOPIC_PATTERN = RealtimeTopics.USER_NOTIFICATIONS_TOPIC;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            handleSend(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        // Priority 1: Authorization header
        String token = extractTokenFromHeaders(accessor);

        // Priority 2: Query param fallback (copied into session attributes by HandshakeInterceptor)
        if (token == null) {
            token = extractTokenFromSessionAttributes(accessor);
        }

        if (token == null || !jwtService.isTokenValid(token)) {
            log.warn("[WS-AUTH] Connection rejected: Invalid or missing token");
            throw new AccessDeniedException("Unauthorized connection");
        }

        try {
            UUID userId = jwtService.extractUserId(token);
            UserDetails userDetails = userDetailsService.loadUserById(userId);
            if (!userDetails.isEnabled()) {
                throw new AccessDeniedException("User account is disabled");
            }
            UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            accessor.setUser(auth);
            log.info("[WS-AUTH] Connection authenticated for userId={}", userId);
        } catch (UsernameNotFoundException | IllegalArgumentException e) {
            log.warn("[WS-AUTH] Connection rejected: User not found");
            throw new AccessDeniedException("Unauthorized connection");
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) {
            log.warn("[WS-AUTH] Anonymous subscription attempt blocked");
            throw new AccessDeniedException("Anonymous subscriptions are disabled");
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher userNotifMatcher = USER_NOTIFICATION_TOPIC_PATTERN.matcher(destination);
        if (userNotifMatcher.matches()) {
            UUID topicUserId;
            try {
                topicUserId = UUID.fromString(userNotifMatcher.group(1));
            } catch (IllegalArgumentException e) {
                throw new AccessDeniedException("Invalid user ID in notification topic");
            }
            CustomUserPrincipal principal = (CustomUserPrincipal) ((UsernamePasswordAuthenticationToken) user).getPrincipal();
            if (!principal.getUserId().equals(topicUserId)) {
                log.warn("[WS-AUTH] User {} attempted unauthorized notification subscription {}", principal.getUserId(), destination);
                throw new AccessDeniedException("Cannot subscribe to another user's notifications");
            }
            log.debug("[WS-AUTH] User {} subscribed to notifications", topicUserId);
            return;
        }

        Matcher matcher = CONVERSATION_TOPIC_PATTERN.matcher(destination);
        if (matcher.matches()) {
            UUID conversationId;
            try {
                conversationId = UUID.fromString(matcher.group(1));
            } catch (IllegalArgumentException e) {
                log.warn("[WS-AUTH] Invalid UUID in destination topic: {}", destination);
                throw new AccessDeniedException("Invalid conversation ID");
            }

            CustomUserPrincipal principal = (CustomUserPrincipal) ((UsernamePasswordAuthenticationToken) user).getPrincipal();
            UUID userId = principal.getUserId();

            if (!participantRepository.isParticipant(conversationId, userId)) {
                log.warn("[WS-AUTH] User {} attempted unauthorized subscription to topic {}", userId, destination);
                throw new AccessDeniedException("You are not a participant of this conversation");
            }
            log.debug("[WS-AUTH] User {} subscribed to topic {}", userId, destination);
        }
    }

    private void handleSend(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) {
            log.warn("[WS-AUTH] Anonymous send attempt blocked");
            throw new AccessDeniedException("Anonymous messages are disabled");
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        // Validate sending to topic/queue is restricted to conversation rooms the user belongs to
        // If client is sending to /app/conversations/{conversationId}/messages or similar
        // We can parse the conversationId and check membership to ensure SEND authorization
        if (destination.startsWith("/topic/users/") && destination.endsWith("/notifications")) {
            throw new AccessDeniedException("Notification channel is read-only");
        }
        if (destination.startsWith("/app/conversations/") || destination.startsWith("/topic/conversations/")) {
            String[] parts = destination.split("/");
            if (parts.length >= 4) {
                UUID conversationId;
                try {
                    conversationId = UUID.fromString(parts[3]);
                } catch (IllegalArgumentException e) {
                    throw new AccessDeniedException("Invalid conversation ID");
                }
                CustomUserPrincipal principal = (CustomUserPrincipal) ((UsernamePasswordAuthenticationToken) user).getPrincipal();
                UUID userId = principal.getUserId();

                if (!participantRepository.isParticipant(conversationId, userId)) {
                    log.warn("[WS-AUTH] User {} attempted unauthorized message send to destination {}", userId, destination);
                    throw new AccessDeniedException("You are not a participant of this conversation");
                }
            }
        }
    }

    private String extractTokenFromHeaders(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String val = authHeaders.get(0);
            if (val.startsWith("Bearer ")) {
                return val.substring(7);
            }
        }
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }
        return null;
    }

    private String extractTokenFromSessionAttributes(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = SimpMessageHeaderAccessor.getSessionAttributes(accessor.getMessageHeaders());
        if (sessionAttributes != null && sessionAttributes.containsKey("token")) {
            return (String) sessionAttributes.get("token");
        }
        return null;
    }
}
