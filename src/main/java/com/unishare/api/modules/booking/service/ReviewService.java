package com.unishare.api.modules.booking.service;

import com.unishare.api.modules.booking.dto.CreateReviewRequest;
import com.unishare.api.modules.booking.dto.RatingSummaryResponse;
import com.unishare.api.modules.booking.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {
    ReviewResponse createReview(UUID reviewerId, UUID bookingId, CreateReviewRequest request);
    Page<ReviewResponse> getReviewsByMentor(UUID mentorId, Pageable pageable);
    Page<ReviewResponse> getReviewsByPackage(UUID packageId, Pageable pageable);
    RatingSummaryResponse getRatingSummary(UUID mentorId);
}
