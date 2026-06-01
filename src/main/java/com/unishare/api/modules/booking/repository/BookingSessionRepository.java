package com.unishare.api.modules.booking.repository;

import com.unishare.api.modules.booking.entity.BookingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BookingSessionRepository extends JpaRepository<BookingSession, UUID> {

        List<BookingSession> findByBookingIdOrderByScheduledAtAsc(UUID bookingId);

        @Query("""
                        SELECT s FROM BookingSession s
                        INNER JOIN Booking b ON s.bookingId = b.id
                        WHERE b.buyerId = :buyerId
                        AND b.status NOT IN ('canceled', 'completed', 'refunded')
                        AND s.status IN :statuses
                        ORDER BY s.scheduledAt ASC
                        """)
        List<BookingSession> findUpcomingSessionsForBuyer(
                        @org.springframework.data.repository.query.Param("buyerId") UUID buyerId,
                        @org.springframework.data.repository.query.Param("statuses") Collection<String> statuses);

        @Query("""
                        SELECT s FROM BookingSession s
                        INNER JOIN Booking b ON s.bookingId = b.id
                        WHERE b.mentorId = :mentorId
                        AND b.status NOT IN ('canceled', 'completed', 'refunded')
                        AND s.status IN :statuses
                        ORDER BY s.scheduledAt ASC
                        """)
        List<BookingSession> findUpcomingSessionsForMentor(
                        @org.springframework.data.repository.query.Param("mentorId") UUID mentorId,
                        @org.springframework.data.repository.query.Param("statuses") Collection<String> statuses);

        @Query("""
                        SELECT COUNT(s) > 0 FROM BookingSession s
                        JOIN Booking b ON s.bookingId = b.id
                        WHERE b.mentorId = :mentorId
                        AND s.id != :sessionId
                        AND s.status IN ('scheduled', 'in_progress')
                        AND s.scheduledAt BETWEEN :start AND :end
                        """)
        boolean existsOverlappingSession(
                        @org.springframework.data.repository.query.Param("mentorId") UUID mentorId,
                        @org.springframework.data.repository.query.Param("sessionId") UUID sessionId,
                        @org.springframework.data.repository.query.Param("start") java.time.Instant start,
                        @org.springframework.data.repository.query.Param("end") java.time.Instant end);

    @Query("SELECT COUNT(s) FROM BookingSession s WHERE s.bookingId = :bookingId AND s.status != 'completed' AND s.status != 'canceled'")
    long countUncompletedSessionsByBookingId(@org.springframework.data.repository.query.Param("bookingId") UUID bookingId);
}
