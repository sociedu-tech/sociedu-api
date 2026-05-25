package com.unishare.api.modules.finance.repository;

import com.unishare.api.modules.finance.entity.PayoutRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, UUID> {

    Page<PayoutRequest> findByMentorIdOrderByCreatedAtDesc(UUID mentorId, Pageable pageable);

    Page<PayoutRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<PayoutRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(p.grossAmount), 0) FROM PayoutRequest p WHERE p.mentorId = :mentorId AND p.status = 'PAID'"
    )
    java.math.BigDecimal calculateTotalWithdrawnByMentor(@org.springframework.data.repository.query.Param("mentorId") UUID mentorId);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(p.grossAmount), 0) FROM PayoutRequest p WHERE p.mentorId = :mentorId AND p.status IN ('PENDING', 'APPROVED', 'PROCESSING')"
    )
    java.math.BigDecimal calculateLockedBalanceByMentor(@org.springframework.data.repository.query.Param("mentorId") UUID mentorId);
}
