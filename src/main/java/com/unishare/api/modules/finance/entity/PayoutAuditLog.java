package com.unishare.api.modules.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payout_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PayoutAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payout_request_id", nullable = false)
    private UUID payoutRequestId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "previous_status", length = 32)
    private String previousStatus;

    @Column(name = "next_status", nullable = false, length = 32)
    private String nextStatus;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
