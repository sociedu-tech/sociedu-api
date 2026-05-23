package com.unishare.api.modules.finance.repository;

import com.unishare.api.modules.finance.entity.PayoutAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PayoutAuditLogRepository extends JpaRepository<PayoutAuditLog, UUID> {

    List<PayoutAuditLog> findByPayoutRequestIdOrderByCreatedAtAsc(UUID payoutRequestId);
}
