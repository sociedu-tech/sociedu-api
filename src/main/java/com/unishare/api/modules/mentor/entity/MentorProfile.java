package com.unishare.api.modules.mentor.entity;

import com.unishare.api.common.constants.MentorVerificationStatuses;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mentor_profiles")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MentorProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    /** Cột phải là text/varchar trên PostgreSQL — tránh map nhầm bytea (lower(bytea) lỗi). */
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "headline", columnDefinition = "TEXT")
    private String headline;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "expertise", columnDefinition = "TEXT")
    private String expertise;

    @Column(name = "base_price")
    private BigDecimal basePrice;

    @Column(name = "rating_avg")
    private Double ratingAvg = 0.0;

    @Column(name = "sessions_completed")
    private Integer sessionsCompleted = 0;

    @Column(name = "rating_count")
    private Integer ratingCount = 0;

    @Column(name = "rating_total")
    private Long ratingTotal = 0L;

    @Version
    private Long version;

    @Column(name = "verification_status")
    private String verificationStatus = MentorVerificationStatuses.PENDING;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
