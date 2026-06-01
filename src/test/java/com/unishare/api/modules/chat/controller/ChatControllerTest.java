package com.unishare.api.modules.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.GlobalExceptionHandler;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.chat.dto.*;
import com.unishare.api.modules.chat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    private UUID userId;
    private CustomUserPrincipal principal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        principal = new CustomUserPrincipal(
                userId,
                "chatuser@gmail.com",
                "hashedpassword",
                List.of("USER"),
                List.of("VIEW_CONVERSATION", "SEND_MESSAGE"),
                true
        );

        objectMapper = new ObjectMapper();

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(CustomUserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                           NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return principal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setCustomArgumentResolvers(principalResolver, new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @DisplayName("POST /api/v1/chat/conversations/direct - Tìm hoặc tạo hội thoại 1-1")
    void findOrCreateDirect_ReturnsSuccess() throws Exception {
        UUID peerId = UUID.randomUUID();
        DirectConversationRequest request = new DirectConversationRequest();
        request.setPeerUserId(peerId);
        request.setContextType("order");
        request.setContextId(UUID.randomUUID());

        ConversationResponse response = ConversationResponse.builder()
                .id(UUID.randomUUID())
                .type("general")
                .build();

        when(chatService.findOrCreateDirectConversation(eq(userId), any(DirectConversationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/chat/conversations/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("general"))
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/conversations - Tạo conversation thành công")
    void createConversation_ReturnsSuccess() throws Exception {
        CreateConversationRequest request = new CreateConversationRequest();
        request.setType("direct");
        request.setBookingId(UUID.randomUUID());
        request.setParticipantUserIds(List.of(UUID.randomUUID()));

        ConversationResponse response = ConversationResponse.builder()
                .id(UUID.randomUUID())
                .type("direct")
                .build();

        when(chatService.createConversation(eq(userId), any(CreateConversationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("direct"))
                .andExpect(jsonPath("$.isSuccess").value(true));

        mockMvc.perform(post("/api/v1/chat/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/conversations - Lấy danh sách conversation")
    void listConversations_ReturnsSuccess() throws Exception {
        ConversationResponse response = ConversationResponse.builder()
                .id(UUID.randomUUID())
                .type("group")
                .build();

        com.unishare.api.common.dto.PageResponse<ConversationResponse> page =
                com.unishare.api.common.dto.PageResponse.<ConversationResponse>builder()
                        .items(List.of(response))
                        .page(0)
                        .size(20)
                        .total(1)
                        .totalPages(1)
                        .build();
        when(chatService.listMyConversations(eq(userId), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/conversations"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].type").value("group"));

        mockMvc.perform(get("/api/v1/chat/conversations"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/conversations/{id} - Lấy chi tiết conversation")
    void getConversation_ReturnsSuccess() throws Exception {
        UUID conversationId = UUID.randomUUID();
        ConversationResponse response = ConversationResponse.builder()
                .id(conversationId)
                .type("direct")
                .build();

        when(chatService.getConversation(userId, conversationId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/conversations/{id}", conversationId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(conversationId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/conversations/{id}/messages - Lấy tin nhắn phân trang")
    void listMessagesPaged_ReturnsSuccess() throws Exception {
        UUID conversationId = UUID.randomUUID();
        ChatMessageResponse msg = ChatMessageResponse.builder()
                .id(UUID.randomUUID())
                .content("Hello")
                .build();

        PageRequest pageable = PageRequest.of(0, 10);
        when(chatService.listMessages(eq(userId), eq(conversationId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(msg), pageable, 1));

        mockMvc.perform(get("/api/v1/conversations/{id}/messages", conversationId)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content").value("Hello"));
    }

    @Test
    @DisplayName("GET /api/v1/chat/conversations/{id}/messages - Lấy tin nhắn dạng list (Tương thích ngược)")
    void listMessagesList_ReturnsSuccess() throws Exception {
        UUID conversationId = UUID.randomUUID();
        ChatMessageResponse msg = ChatMessageResponse.builder()
                .id(UUID.randomUUID())
                .content("Hi")
                .build();

        when(chatService.listMessages(userId, conversationId)).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/v1/chat/conversations/{id}/messages", conversationId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("Hi"));
    }
}
