package com.unishare.api.modules.notification.repository;

import com.unishare.api.modules.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(UUID userId);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.userId = :userId
              AND n.type NOT IN :excludeTypes
              AND (n.referenceType IS NULL OR n.referenceType <> 'conversation')
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> findActionInbox(
            @Param("userId") UUID userId,
            @Param("excludeTypes") List<String> excludeTypes,
            Pageable pageable);

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.userId = :userId
              AND n.isRead = false
              AND n.type NOT IN :excludeTypes
              AND (n.referenceType IS NULL OR n.referenceType <> 'conversation')
            """)
    long countActionUnread(@Param("userId") UUID userId, @Param("excludeTypes") List<String> excludeTypes);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") UUID userId, @Param("readAt") Instant readAt);
}
