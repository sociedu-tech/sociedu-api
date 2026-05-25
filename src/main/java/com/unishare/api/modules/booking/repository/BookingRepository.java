package com.unishare.api.modules.booking.repository;

import com.unishare.api.modules.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByOrderId(UUID orderId);

    List<Booking> findByBuyerId(UUID buyerId);

    List<Booking> findByMentorId(UUID mentorId);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(o.totalAmount), 0) FROM Booking b, Order o WHERE b.orderId = o.id AND b.mentorId = :mentorId AND b.status = 'completed'"
    )
    java.math.BigDecimal calculateTotalEarnedByMentor(@org.springframework.data.repository.query.Param("mentorId") UUID mentorId);
}
