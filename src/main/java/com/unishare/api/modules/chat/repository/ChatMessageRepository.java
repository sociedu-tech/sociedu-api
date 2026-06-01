package com.unishare.api.modules.chat.repository;

import com.unishare.api.modules.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            SELECT m FROM ChatMessage m
            WHERE m.conversationId IN :conversationIds
            AND NOT EXISTS (
                SELECT 1 FROM ChatMessage m2
                WHERE m2.conversationId = m.conversationId
                  AND m2.createdAt > m.createdAt
            )
            """)
    List<ChatMessage> findLatestByConversationIds(
            @org.springframework.data.repository.query.Param("conversationIds") Collection<UUID> conversationIds);
}
