package com.unishare.api.modules.payment.entity;

import com.unishare.api.common.constants.PaymentTransactionStatuses;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(length = 50, nullable = false)
    private String provider;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "status", nullable = false)
    private String status = PaymentTransactionStatuses.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String rawResponse;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
