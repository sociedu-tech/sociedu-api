package com.unishare.api.modules.booking.repository;

import com.unishare.api.modules.booking.entity.BookingReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookingReviewRepository extends JpaRepository<BookingReview, UUID> {

    Page<BookingReview> findByMentorIdAndDeletedAtIsNull(UUID mentorId, Pageable pageable);

    Page<BookingReview> findByPackageIdAndDeletedAtIsNull(UUID packageId, Pageable pageable);

    boolean existsByBookingIdAndReviewerId(UUID bookingId, UUID reviewerId);

    @Query("SELECT r.rating, COUNT(r) FROM BookingReview r WHERE r.mentorId = :mentorId AND r.deletedAt IS NULL GROUP BY r.rating")
    List<Object[]> countReviewsByRatingForMentor(@Param("mentorId") UUID mentorId);
}
