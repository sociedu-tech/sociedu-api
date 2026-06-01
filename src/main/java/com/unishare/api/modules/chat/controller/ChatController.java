package com.unishare.api.modules.chat.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.chat.dto.*;
import com.unishare.api.modules.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Tag(name = "Chat")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Tìm hoặc tạo hội thoại 1-1 (general) với peer")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = {"/api/v1/chat/conversations/direct", "/api/v1/conversations/direct"})
    public ResponseEntity<ApiResponse<ConversationResponse>> findOrCreateDirect(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody DirectConversationRequest request) {
        return ResponseEntity.ok(ApiResponse.<ConversationResponse>build()
                .withData(chatService.findOrCreateDirectConversation(principal.getUserId(), request)));
    }

    @Operation(summary = "Tạo conversation")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = {"/api/v1/chat/conversations", "/api/v1/conversations"})
    public ResponseEntity<ApiResponse<ConversationResponse>> create(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreateConversationRequest request) {
        return ResponseEntity.ok(ApiResponse.<ConversationResponse>build()
                .withData(chatService.createConversation(principal.getUserId(), request)));
    }

    @Operation(summary = "Danh sách conversation của tôi")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = {"/api/v1/chat/conversations", "/api/v1/conversations"})
    public ResponseEntity<ApiResponse<PageResponse<ConversationResponse>>> listConversations(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PageableDefault(size = 20, sort = "joinedAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<ConversationResponse>>build()
                .withData(chatService.listMyConversations(principal.getUserId(), pageable)));
    }

    @Operation(summary = "Lấy chi tiết conversation")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = {"/api/v1/chat/conversations/{conversationId}", "/api/v1/conversations/{conversationId}"})
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID conversationId) {
        return ResponseEntity.ok(ApiResponse.<ConversationResponse>build()
                .withData(chatService.getConversation(principal.getUserId(), conversationId)));
    }

    @Operation(summary = "Tin nhắn trong conversation (Không phân trang - Tương thích ngược)", deprecated = true)
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/chat/conversations/{conversationId}/messages")
    @Deprecated
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> listMessagesOld(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID conversationId) {
        return ResponseEntity.ok(ApiResponse.<List<ChatMessageResponse>>build()
                .withData(chatService.listMessages(principal.getUserId(), conversationId)));
    }

    @Operation(summary = "Tin nhắn trong conversation (Có phân trang)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> listMessages(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID conversationId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<ChatMessageResponse>>build()
                .withData(chatService.listMessages(principal.getUserId(), conversationId, pageable)));
    }

    @Operation(summary = "Gửi tin nhắn")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = {"/api/v1/chat/conversations/{conversationId}/messages", "/api/v1/conversations/{conversationId}/messages"})
    public ResponseEntity<ApiResponse<ChatMessageResponse>> send(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID conversationId,
            @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.<ChatMessageResponse>build()
                .withData(chatService.sendMessage(principal.getUserId(), conversationId, request)));
    }
}
