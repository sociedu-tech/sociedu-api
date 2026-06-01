package com.unishare.api.modules.booking.entity;

import com.unishare.api.common.constants.BookingStatuses;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", unique = true)
    private UUID orderId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "mentor_id", nullable = false)
    private UUID mentorId;

    @Column(name = "package_id", nullable = false)
    private UUID packageId;

    @Column(nullable = false)
    private String status = BookingStatuses.PENDING;

    @Column(name = "progress_percent")
    private Integer progressPercent;

    @Column(name = "created_at")
    private Instant createdAt;

    @Version
    private Long version;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
