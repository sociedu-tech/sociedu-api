package com.unishare.api.modules.booking.service.impl;

import com.unishare.api.common.dto.AppException;
import com.unishare.api.modules.booking.dto.CreateReviewRequest;
import com.unishare.api.modules.booking.dto.RatingSummaryResponse;
import com.unishare.api.modules.booking.dto.ReviewResponse;
import com.unishare.api.modules.booking.entity.Booking;
import com.unishare.api.modules.booking.entity.BookingReview;
import com.unishare.api.modules.booking.exception.BookingErrorCode;
import com.unishare.api.modules.booking.repository.BookingRepository;
import com.unishare.api.modules.booking.repository.BookingReviewRepository;
import com.unishare.api.modules.mentor.entity.MentorProfile;
import com.unishare.api.modules.mentor.exception.MentorErrorCode;
import com.unishare.api.modules.mentor.repository.MentorProfileRepository;
import com.unishare.api.modules.user.entity.UserProfile;
import com.unishare.api.modules.user.repository.UserProfileRepository;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingReviewRepository bookingReviewRepository;

    @Mock
    private MentorProfileRepository mentorProfileRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private UUID reviewerId;
    private UUID bookingId;
    private UUID mentorId;
    private UUID packageId;
    private Booking booking;
    private MentorProfile mentorProfile;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        reviewerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        mentorId = UUID.randomUUID();
        packageId = UUID.randomUUID();

        booking = new Booking();
        booking.setId(bookingId);
        booking.setBuyerId(reviewerId);
        booking.setMentorId(mentorId);
        booking.setPackageId(packageId);
        booking.setStatus("completed");

        mentorProfile = new MentorProfile();
        mentorProfile.setUserId(mentorId);
        mentorProfile.setRatingAvg(4.5);
        mentorProfile.setRatingCount(10);
        mentorProfile.setRatingTotal(45L);

        userProfile = new UserProfile();
        userProfile.setUserId(reviewerId);
        userProfile.setFirstName("Huy");
        userProfile.setLastName("Nguyen");
    }

    @Test
    void createReview_Success() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);
        request.setComment("Tuyệt vời!");

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingReviewRepository.existsByBookingIdAndReviewerId(bookingId, reviewerId)).thenReturn(false);
        when(mentorProfileRepository.findById(mentorId)).thenReturn(Optional.of(mentorProfile));
        when(userProfileRepository.findById(reviewerId)).thenReturn(Optional.of(userProfile));

        BookingReview savedReview = new BookingReview();
        savedReview.setId(UUID.randomUUID());
        savedReview.setBookingId(bookingId);
        savedReview.setReviewerId(reviewerId);
        savedReview.setMentorId(mentorId);
        savedReview.setPackageId(packageId);
        savedReview.setRating(5);
        savedReview.setComment("Tuyệt vời!");

        when(bookingReviewRepository.save(any(BookingReview.class))).thenReturn(savedReview);

        ReviewResponse response = reviewService.createReview(reviewerId, bookingId, request);

        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals("Tuyệt vời!", response.getComment());
        assertEquals("Huy Nguyen", response.getReviewerName());

        verify(bookingReviewRepository).save(any(BookingReview.class));
        verify(mentorProfileRepository).updateRatingIncrementally(mentorId, 5);
    }

    @Test
    void createReview_BookingNotFound_ThrowsException() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                reviewService.createReview(reviewerId, bookingId, request));

        assertEquals(BookingErrorCode.BOOKING_NOT_FOUND.getCode(), ex.getExceptionCode().getCode());
    }

    @Test
    void createReview_NotBuyer_ThrowsException() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);

        booking.setBuyerId(UUID.randomUUID()); // other buyer

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        AppException ex = assertThrows(AppException.class, () ->
                reviewService.createReview(reviewerId, bookingId, request));

        assertEquals(BookingErrorCode.REVIEW_ACCESS_DENIED.getCode(), ex.getExceptionCode().getCode());
    }

    @Test
    void createReview_BookingNotCompleted_ThrowsException() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);

        booking.setStatus("canceled");

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        AppException ex = assertThrows(AppException.class, () ->
                reviewService.createReview(reviewerId, bookingId, request));

        assertEquals(BookingErrorCode.BOOKING_NOT_COMPLETED.getCode(), ex.getExceptionCode().getCode());
    }

    @Test
    void createReview_BookingInProgress_Success() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);
        request.setComment("Tuyệt vời!");

        booking.setStatus("in_progress");

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingReviewRepository.existsByBookingIdAndReviewerId(bookingId, reviewerId)).thenReturn(false);
        when(mentorProfileRepository.findById(mentorId)).thenReturn(Optional.of(mentorProfile));
        when(userProfileRepository.findById(reviewerId)).thenReturn(Optional.of(userProfile));

        BookingReview savedReview = new BookingReview();
        savedReview.setId(UUID.randomUUID());
        savedReview.setBookingId(bookingId);
        savedReview.setReviewerId(reviewerId);
        savedReview.setMentorId(mentorId);
        savedReview.setPackageId(packageId);
        savedReview.setRating(5);
        savedReview.setComment("Tuyệt vời!");

        when(bookingReviewRepository.save(any(BookingReview.class))).thenReturn(savedReview);

        ReviewResponse response = reviewService.createReview(reviewerId, bookingId, request);

        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals("Tuyệt vời!", response.getComment());
        assertEquals("Huy Nguyen", response.getReviewerName());

        verify(bookingReviewRepository).save(any(BookingReview.class));
        verify(mentorProfileRepository).updateRatingIncrementally(mentorId, 5);
    }

    @Test
    void createReview_AlreadyReviewed_ThrowsException() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingReviewRepository.existsByBookingIdAndReviewerId(bookingId, reviewerId)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () ->
                reviewService.createReview(reviewerId, bookingId, request));

        assertEquals(BookingErrorCode.REVIEW_ALREADY_EXISTS.getCode(), ex.getExceptionCode().getCode());
    }

    @Test
    void getRatingSummary_Success() {
        when(mentorProfileRepository.findById(mentorId)).thenReturn(Optional.of(mentorProfile));

        List<Object[]> counts = new ArrayList<>();
        counts.add(new Object[]{5, 6L});
        counts.add(new Object[]{4, 4L});
        when(bookingReviewRepository.countReviewsByRatingForMentor(mentorId)).thenReturn(counts);

        RatingSummaryResponse response = reviewService.getRatingSummary(mentorId);

        assertNotNull(response);
        assertEquals(4.5, response.getRatingAvg());
        assertEquals(10, response.getRatingCount());
        assertEquals(6L, response.getDistribution().get(5));
        assertEquals(4L, response.getDistribution().get(4));
        assertEquals(0L, response.getDistribution().get(1));
    }
}
