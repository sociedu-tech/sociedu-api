package com.unishare.api.modules.chat.controller;

import com.unishare.api.infrastructure.security.JwtService;
import com.unishare.api.modules.auth.entity.User;
import com.unishare.api.modules.auth.repository.UserRepository;
import com.unishare.api.modules.auth.repository.UserCredentialRepository;
import com.unishare.api.modules.auth.entity.Role;
import com.unishare.api.modules.auth.entity.UserRole;
import com.unishare.api.common.constants.Roles;
import com.unishare.api.modules.chat.repository.ConversationParticipantRepository;
import com.unishare.api.modules.chat.repository.ConversationRepository;
import com.unishare.api.modules.chat.repository.ChatMessageRepository;
import com.unishare.api.modules.chat.dto.ChatEventEnvelope;
import com.unishare.api.common.constants.UserStatuses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketChatIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserCredentialRepository userCredentialRepository;

    @MockitoBean
    private ConversationParticipantRepository participantRepository;

    @MockitoBean
    private ConversationRepository conversationRepository;

    @MockitoBean
    private ChatMessageRepository messageRepository;

    private String websocketUrl;
    private WebSocketStompClient stompClient;

    private UUID activeUserId;
    private String activeUserToken;

    @BeforeEach
    void setUp() {
        websocketUrl = "ws://localhost:" + port + "/api/v1/ws";
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        activeUserId = UUID.randomUUID();
        activeUserToken = jwtService.generateAccessToken(activeUserId, List.of("ROLE_USER"));

        // Mock User Details loading for security context
        User user = new User();
        user.setId(activeUserId);
        user.setEmail("active@unishare.com");
        user.setStatus(UserStatuses.ACTIVE);
        user.setEmailVerified(true);

        Role userRole = new Role();
        userRole.setId(UUID.randomUUID());
        userRole.setName(Roles.USER);
        UserRole ur = new UserRole();
        ur.setRole(userRole);
        ur.getId().setRoleId(userRole.getId());
        ur.getId().setUserId(activeUserId);
        user.addUserRole(ur);

        when(userRepository.findById(activeUserId)).thenReturn(Optional.of(user));
        when(userCredentialRepository.findByUserId(activeUserId)).thenReturn(Optional.empty());
    }

    @Test
    void testConnect_SuccessWithValidToken() throws Exception {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + activeUserToken);

        StompSession session = stompClient.connectAsync(websocketUrl, new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        assertTrue(session.isConnected());
        session.disconnect();
    }

    @Test
    void testConnect_FailureWithInvalidToken() {
        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer invalidTokenString");

        Exception exception = assertThrows(Exception.class, () -> {
            stompClient.connectAsync(websocketUrl, new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() {})
                    .get(5, TimeUnit.SECONDS);
        });
        
        assertNotNull(exception);
    }

    @Test
    void testSubscribe_SuccessWhenParticipant() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(participantRepository.isParticipant(conversationId, activeUserId)).thenReturn(true);

        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + activeUserToken);

        StompSession session = stompClient.connectAsync(websocketUrl, new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        assertTrue(session.isConnected());

        StompHeaders subHeaders = new StompHeaders();
        subHeaders.setDestination("/topic/conversations/" + conversationId);

        BlockingQueue<ChatEventEnvelope> blockingQueue = new LinkedBlockingDeque<>();
        session.subscribe(subHeaders, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatEventEnvelope.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                blockingQueue.offer((ChatEventEnvelope) payload);
            }
        });

        // The subscription should succeed and not disconnect
        Thread.sleep(1000);
        assertTrue(session.isConnected());
        session.disconnect();
    }

    @Test
    void testSubscribe_FailureWhenNotParticipant() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(participantRepository.isParticipant(conversationId, activeUserId)).thenReturn(false);

        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + activeUserToken);

        StompSession session = stompClient.connectAsync(websocketUrl, new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        assertTrue(session.isConnected());

        StompHeaders subHeaders = new StompHeaders();
        subHeaders.setDestination("/topic/conversations/" + conversationId);

        session.subscribe(subHeaders, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatEventEnvelope.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {}
        });

        // Wait a short time, subscription interceptor throws AccessDeniedException
        // which typically closes the socket session
        Thread.sleep(2000);
        assertFalse(session.isConnected());
    }
}
