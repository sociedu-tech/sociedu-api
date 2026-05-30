package com.unishare.api.modules.mentorapplication.entity;

import com.unishare.api.common.constants.MentorRequestStatuses;
import com.unishare.api.modules.mentorapplication.dto.MentorApplicationCertificate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mentor_requests")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MentorApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String status = MentorRequestStatuses.SUBMITTED;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(columnDefinition = "TEXT")
    private String headline;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(columnDefinition = "TEXT")
    private String bio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> expertise = new ArrayList<>();

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;

    @Column(name = "cv_file_id")
    private UUID cvFileId;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "cv_url", columnDefinition = "TEXT")
    private String cvUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "portfolio_urls", columnDefinition = "jsonb")
    private List<String> portfolioUrls = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<MentorApplicationCertificate> certificates = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(columnDefinition = "TEXT")
    private String reason;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "resubmit_count")
    private Integer resubmitCount = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
