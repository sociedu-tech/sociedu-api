package com.unishare.api.modules.booking.repository;

import com.unishare.api.modules.booking.entity.SessionReportRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionReportRequestRepository extends JpaRepository<SessionReportRequest, UUID> {
    List<SessionReportRequest> findByBookingIdOrderByCreatedAtDesc(UUID bookingId);
    Page<SessionReportRequest> findByMenteeIdOrderByCreatedAtDesc(UUID menteeId, Pageable pageable);
    Page<SessionReportRequest> findByMentorIdOrderByCreatedAtDesc(UUID mentorId, Pageable pageable);
}
