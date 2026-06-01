package com.unishare.api.modules.chat.repository;

import com.unishare.api.modules.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
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

    @org.springframework.data.jpa.repository.Query("""
            SELECT COUNT(m) FROM ChatMessage m
            WHERE m.conversationId = :conversationId
              AND m.senderId <> :userId
              AND m.createdAt > :since
            """)
    long countUnreadSince(
            @org.springframework.data.repository.query.Param("conversationId") UUID conversationId,
            @org.springframework.data.repository.query.Param("userId") UUID userId,
            @org.springframework.data.repository.query.Param("since") Instant since);

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT m.conversation_id, COUNT(*)::bigint
            FROM messages m
            INNER JOIN conversation_participants cp
                ON cp.conversation_id = m.conversation_id AND cp.user_id = :userId
            WHERE m.conversation_id IN (:conversationIds)
              AND m.sender_id <> :userId
              AND m.created_at > COALESCE(cp.last_read_at, cp.joined_at, TIMESTAMP '1970-01-01 00:00:00')
            GROUP BY m.conversation_id
            """, nativeQuery = true)
    List<Object[]> countUnreadByConversationIdsForUser(
            @org.springframework.data.repository.query.Param("conversationIds") Collection<UUID> conversationIds,
            @org.springframework.data.repository.query.Param("userId") UUID userId);
}
