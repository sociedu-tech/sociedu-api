package com.unishare.api.modules.booking.controller;

import com.unishare.api.common.dto.ApiResponse;
import com.unishare.api.config.OpenApiConfig;
import com.unishare.api.infrastructure.security.CustomUserPrincipal;
import com.unishare.api.modules.booking.dto.CreateReviewRequest;
import com.unishare.api.modules.booking.dto.RatingSummaryResponse;
import com.unishare.api.modules.booking.dto.ReviewResponse;
import com.unishare.api.modules.booking.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Booking Reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Gửi đánh giá booking")
    @SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
    @PreAuthorize("hasAnyRole('USER', 'MENTOR', 'ADMIN')")
    @PostMapping("/api/v1/bookings/{bookingId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable("bookingId") UUID bookingId,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.<ReviewResponse>build()
                .withData(reviewService.createReview(principal.getUserId(), bookingId, request))
                .withMessage("Gửi đánh giá thành công"));
    }

    @Operation(summary = "Lấy danh sách đánh giá của mentor")
    @SecurityRequirements(value = {})
    @GetMapping("/api/v1/mentors/{mentorId}/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getMentorReviews(
            @PathVariable("mentorId") UUID mentorId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<ReviewResponse>>build()
                .withData(reviewService.getReviewsByMentor(mentorId, pageable)));
    }

    @Operation(summary = "Lấy tóm tắt đánh giá của mentor")
    @SecurityRequirements(value = {})
    @GetMapping("/api/v1/mentors/{mentorId}/rating-summary")
    public ResponseEntity<ApiResponse<RatingSummaryResponse>> getMentorRatingSummary(
            @PathVariable("mentorId") UUID mentorId) {
        return ResponseEntity.ok(ApiResponse.<RatingSummaryResponse>build()
                .withData(reviewService.getRatingSummary(mentorId)));
    }
}
