package com.unishare.api.modules.service.repository;

import com.unishare.api.modules.service.entity.ServicePackage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, UUID> {
        List<ServicePackage> findByMentorId(UUID mentorId);

        Page<ServicePackage> findByMentorId(UUID mentorId, Pageable pageable);

        Page<ServicePackage> findByMentorIdAndIsActiveTrueAndDeletedAtIsNull(UUID mentorId, Pageable pageable);

        Page<ServicePackage> findByIsActiveTrueAndDeletedAtIsNull(Pageable pageable);

        @Query("""
                        SELECT p FROM ServicePackage p
                        WHERE p.id = :id
                        AND p.isActive = true
                        AND p.deletedAt IS NULL
                        """)
        Optional<ServicePackage> findActiveById(@Param("id") UUID id);

        @Query("""
                        SELECT p FROM ServicePackage p
                        WHERE p.isActive = true
                        AND p.deletedAt IS NULL
                        AND (:mentorId IS NULL OR p.mentorId = :mentorId)
                        AND (CAST(:keyword AS String) IS NULL
                             OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%'))
                             OR LOWER(CAST(p.description AS String)) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))
                        """)
        Page<ServicePackage> searchActivePackages(
                        @Param("mentorId") UUID mentorId,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("""
                        SELECT p FROM ServicePackage p
                        WHERE p.mentorId = :mentorId
                        AND p.isActive = true
                        AND p.deletedAt IS NULL
                        AND (CAST(:keyword AS String) IS NULL
                             OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%'))
                             OR LOWER(CAST(p.description AS String)) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))
                        """)
        Page<ServicePackage> searchActiveByMentorId(
                        @Param("mentorId") UUID mentorId,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("""
                        SELECT p FROM ServicePackage p
                        WHERE p.mentorId = :mentorId
                        AND (CAST(:keyword AS String) IS NULL
                             OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%'))
                             OR LOWER(CAST(p.description AS String)) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))
                        """)
        Page<ServicePackage> searchByMentorId(
                        @Param("mentorId") UUID mentorId,
                        @Param("keyword") String keyword,
                        Pageable pageable);
}
