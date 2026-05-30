package com.unishare.api.modules.mentorapplication.repository;

import com.unishare.api.modules.mentorapplication.entity.MentorApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface MentorApplicationRepository extends JpaRepository<MentorApplication, UUID> {

    Optional<MentorApplication> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndStatusIn(UUID userId, Collection<String> statuses);

    @Query(value = """
            SELECT *
            FROM mentor_requests m
            WHERE (CAST(:status AS text) IS NULL OR m.status = :status)
              AND (CAST(:q AS text) IS NULL OR CAST(:q AS text) = ''
                   OR m.headline ILIKE CONCAT('%', CAST(:q AS text), '%')
                   OR m.bio ILIKE CONCAT('%', CAST(:q AS text), '%'))
            ORDER BY m.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM mentor_requests m
            WHERE (CAST(:status AS text) IS NULL OR m.status = :status)
              AND (CAST(:q AS text) IS NULL OR CAST(:q AS text) = ''
                   OR m.headline ILIKE CONCAT('%', CAST(:q AS text), '%')
                   OR m.bio ILIKE CONCAT('%', CAST(:q AS text), '%'))
            """,
            nativeQuery = true)
    Page<MentorApplication> searchAdmin(
            @Param("status") String status,
            @Param("q") String q,
            Pageable pageable);
}
