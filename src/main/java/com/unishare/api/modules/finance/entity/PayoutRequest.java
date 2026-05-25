package com.unishare.api.modules.finance.entity;

import com.unishare.api.infrastructure.security.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payout_requests")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PayoutRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mentor_id", nullable = false)
    private UUID mentorId;

    @Column(name = "gross_amount", nullable = false)
    private BigDecimal grossAmount;

    @Column(name = "platform_fee_rate", nullable = false)
    private BigDecimal platformFeeRate;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @Column(nullable = false, length = 32)
    private String status = "PENDING"; // PENDING, APPROVED, PROCESSING, PAID, REJECTED, FAILED

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "account_number", nullable = false, length = 255)
    private String accountNumber;

    @Column(name = "account_holder", nullable = false, length = 100)
    private String accountHolder;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "transaction_reference", length = 255)
    private String transactionReference;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}
