package com.unishare.api.modules.chat.repository;

import com.unishare.api.modules.chat.entity.ConversationParticipant;
import com.unishare.api.modules.chat.entity.ConversationParticipantId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, ConversationParticipantId> {

    @Query("SELECT cp.id.conversationId FROM ConversationParticipant cp WHERE cp.id.userId = :userId")
    List<UUID> findConversationIdsByUserId(@Param("userId") UUID userId);

    Page<ConversationParticipant> findById_UserId(UUID userId, Pageable pageable);

    @Query(value = """
            SELECT cp.conversation_id
            FROM conversation_participants cp
            INNER JOIN conversations c ON c.id = cp.conversation_id
            LEFT JOIN (
                SELECT conversation_id, MAX(created_at) AS last_at
                FROM messages
                GROUP BY conversation_id
            ) lm ON lm.conversation_id = cp.conversation_id
            WHERE cp.user_id = :userId
            ORDER BY COALESCE(lm.last_at, c.created_at) DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM conversation_participants cp
            WHERE cp.user_id = :userId
            """,
            nativeQuery = true)
    Page<UUID> findConversationIdsForUserOrderByRecentActivity(
            @Param("userId") UUID userId,
            Pageable pageable);

    @Query("SELECT COUNT(c) > 0 FROM ConversationParticipant c WHERE c.id.conversationId = :cid AND c.id.userId = :uid")
    boolean isParticipant(@Param("cid") UUID conversationId, @Param("uid") UUID userId);

    @Query("""
            SELECT cp1.id.conversationId FROM ConversationParticipant cp1
            JOIN ConversationParticipant cp2 ON cp1.id.conversationId = cp2.id.conversationId
            JOIN Conversation c ON c.id = cp1.id.conversationId
            WHERE cp1.id.userId = :userA
              AND cp2.id.userId = :userB
              AND c.type = 'general'
              AND (SELECT COUNT(p) FROM ConversationParticipant p WHERE p.id.conversationId = c.id) = 2
            """)
    java.util.Optional<UUID> findDirectGeneralConversationId(
            @Param("userA") UUID userA,
            @Param("userB") UUID userB);

    @Query("SELECT cp.id.userId FROM ConversationParticipant cp WHERE cp.id.conversationId = :conversationId")
    List<UUID> findUserIdsByConversationId(@Param("conversationId") UUID conversationId);
}
