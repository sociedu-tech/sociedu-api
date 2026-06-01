package com.unishare.api.modules.booking.service.impl;

import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.event.BookingReviewCreatedEvent;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.modules.booking.dto.CreateReviewRequest;
import com.unishare.api.modules.booking.dto.RatingSummaryResponse;
import com.unishare.api.modules.booking.dto.ReviewResponse;
import com.unishare.api.modules.booking.entity.Booking;
import com.unishare.api.modules.booking.entity.BookingReview;
import com.unishare.api.modules.booking.exception.BookingErrorCode;
import com.unishare.api.modules.booking.repository.BookingRepository;
import com.unishare.api.modules.booking.repository.BookingReviewRepository;
import com.unishare.api.modules.booking.service.ReviewService;
import com.unishare.api.modules.mentor.entity.MentorProfile;
import com.unishare.api.modules.mentor.exception.MentorErrorCode;
import com.unishare.api.modules.mentor.repository.MentorProfileRepository;
import com.unishare.api.modules.user.entity.UserProfile;
import com.unishare.api.modules.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final BookingRepository bookingRepository;
    private final BookingReviewRepository bookingReviewRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final UserProfileRepository userProfileRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    @Transactional
    public ReviewResponse createReview(UUID reviewerId, UUID bookingId, CreateReviewRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND, "Booking not found"));

        if (!booking.getBuyerId().equals(reviewerId)) {
            throw new AppException(BookingErrorCode.REVIEW_ACCESS_DENIED, "Only the buyer can review this booking");
        }

        if (!"completed".equalsIgnoreCase(booking.getStatus())) {
            throw new AppException(BookingErrorCode.BOOKING_NOT_COMPLETED, "Cannot review a booking that is not completed");
        }

        if (bookingReviewRepository.existsByBookingIdAndReviewerId(bookingId, reviewerId)) {
            throw new AppException(BookingErrorCode.REVIEW_ALREADY_EXISTS, "You have already reviewed this booking");
        }

        // Verify mentor exists
        mentorProfileRepository.findById(booking.getMentorId())
                .orElseThrow(() -> new AppException(MentorErrorCode.MENTOR_NOT_FOUND, "Mentor profile not found"));

        // Save review
        BookingReview review = new BookingReview();
        review.setBookingId(bookingId);
        review.setMentorId(booking.getMentorId());
        review.setPackageId(booking.getPackageId());
        review.setReviewerId(reviewerId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        review = bookingReviewRepository.save(review);

        // Atomic update of mentor rating
        mentorProfileRepository.updateRatingIncrementally(booking.getMentorId(), request.getRating());

        eventPublisher.publish(new BookingReviewCreatedEvent(
                review.getId(),
                review.getBookingId(),
                review.getMentorId(),
                review.getReviewerId(),
                review.getRating(),
                review.getComment()
        ));

        String reviewerName = userProfileRepository.findById(reviewerId)
                .map(UserProfile::getDisplayName)
                .orElse("Người dùng");

        return mapToResponse(review, reviewerName);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByMentor(UUID mentorId, Pageable pageable) {
        Page<BookingReview> reviewsPage = bookingReviewRepository.findByMentorIdAndDeletedAtIsNull(mentorId, pageable);
        return mapPageToResponses(reviewsPage);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByPackage(UUID packageId, Pageable pageable) {
        Page<BookingReview> reviewsPage = bookingReviewRepository.findByPackageIdAndDeletedAtIsNull(packageId, pageable);
        return mapPageToResponses(reviewsPage);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryResponse getRatingSummary(UUID mentorId) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new AppException(MentorErrorCode.MENTOR_NOT_FOUND, "Mentor profile not found"));

        List<Object[]> counts = bookingReviewRepository.countReviewsByRatingForMentor(mentorId);
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        for (Object[] row : counts) {
            Integer rating = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            distribution.put(rating, count);
        }

        return RatingSummaryResponse.builder()
                .ratingAvg(profile.getRatingAvg() != null ? profile.getRatingAvg() : 0.0)
                .ratingCount(profile.getRatingCount() != null ? profile.getRatingCount() : 0)
                .distribution(distribution)
                .build();
    }

    private Page<ReviewResponse> mapPageToResponses(Page<BookingReview> reviewsPage) {
        if (reviewsPage.isEmpty()) {
            return Page.empty(reviewsPage.getPageable());
        }

        Set<UUID> reviewerIds = reviewsPage.getContent().stream()
                .map(BookingReview::getReviewerId)
                .collect(Collectors.toSet());

        Map<UUID, String> reviewerNames = userProfileRepository.findAllById(reviewerIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getDisplayName));

        List<ReviewResponse> responses = reviewsPage.getContent().stream()
                .map(review -> {
                    String name = reviewerNames.getOrDefault(review.getReviewerId(), "Người dùng");
                    return mapToResponse(review, name);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responses, reviewsPage.getPageable(), reviewsPage.getTotalElements());
    }

    private ReviewResponse mapToResponse(BookingReview review, String reviewerName) {
        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBookingId())
                .mentorId(review.getMentorId())
                .packageId(review.getPackageId())
                .reviewerId(review.getReviewerId())
                .reviewerName(reviewerName)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .editedAt(review.getEditedAt())
                .build();
    }
}
