package com.unishare.api.modules.booking.entity;

import com.unishare.api.common.constants.SessionStatuses;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_sessions")
@Getter
@Setter
@NoArgsConstructor
public class BookingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "curriculum_id")
    private UUID curriculumId;

    private String title;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(nullable = false)
    private String status = SessionStatuses.PENDING;

    @Column(name = "meeting_url", columnDefinition = "TEXT")
    private String meetingUrl;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "actual_started_at")
    private Instant actualStartedAt;

    @Column(name = "actual_ended_at")
    private Instant actualEndedAt;

    @Column(name = "canceled_by")
    private UUID canceledBy;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "mentee_completion_ack")
    private Boolean menteeCompletionAck;

    @Column(name = "mentor_completion_ack")
    private Boolean mentorCompletionAck;

    @Column(name = "mentee_ack_at")
    private Instant menteeAckAt;

    @Column(name = "mentor_ack_at")
    private Instant mentorAckAt;

    @Version
    private Long version;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
