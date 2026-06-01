package com.unishare.api.modules.booking.service.impl;

import com.unishare.api.common.constants.BookingStatuses;
import com.unishare.api.common.constants.SessionStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.modules.booking.dto.UpdateSessionRequest;
import com.unishare.api.modules.booking.entity.Booking;
import com.unishare.api.modules.booking.entity.BookingSession;
import com.unishare.api.modules.booking.exception.BookingErrorCode;
import com.unishare.api.modules.booking.repository.BookingRepository;
import com.unishare.api.modules.booking.repository.BookingSessionEvidenceRepository;
import com.unishare.api.modules.booking.repository.BookingSessionRepository;
import com.unishare.api.modules.booking.policy.SessionStatusTransitionPolicy;
import com.unishare.api.modules.file.service.FileService;
import com.unishare.api.modules.order.service.OrderService;
import com.unishare.api.modules.service.service.CatalogReadService;
import com.unishare.api.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSessionRepository sessionRepository;

    @Mock
    private BookingSessionEvidenceRepository evidenceRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private CatalogReadService catalogReadService;

    @Mock
    private FileService fileService;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private UserService userService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private UUID mentorId;
    private UUID buyerId;
    private UUID bookingId;
    private UUID sessionId;
    private Booking booking;
    private BookingSession session;

    @BeforeEach
    void setUp() {
        mentorId = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        sessionId = UUID.randomUUID();

        booking = new Booking();
        booking.setId(bookingId);
        booking.setMentorId(mentorId);
        booking.setBuyerId(buyerId);
        booking.setStatus(BookingStatuses.PENDING);

        session = new BookingSession();
        session.setId(sessionId);
        session.setBookingId(bookingId);
        session.setStatus(SessionStatuses.PENDING);
    }

    @Test
    void updateSession_ShouldThrowException_WhenMenteeTriesToUpdateSchedule() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        UpdateSessionRequest req = new UpdateSessionRequest();
        req.setScheduledAt(Instant.now().plus(1, ChronoUnit.DAYS));

        AppException exception = assertThrows(AppException.class, () -> 
            bookingService.updateSession(bookingId, sessionId, buyerId, req)
        );

        assertEquals(BookingErrorCode.BOOKING_ACCESS_DENIED.getCode(), exception.getExceptionCode().getCode());
    }

    @Test
    void updateSession_ShouldUpdateScheduleAndTransitionToScheduled_WhenMentorUpdates() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.existsOverlappingSession(any(), any(), any(), any())).thenReturn(false);

        UpdateSessionRequest req = new UpdateSessionRequest();
        Instant scheduledAt = Instant.now().plus(1, ChronoUnit.DAYS);
        req.setScheduledAt(scheduledAt);
        req.setMeetingUrl("https://meet.google.com/abc");

        bookingService.updateSession(bookingId, sessionId, mentorId, req);

        assertEquals(scheduledAt, session.getScheduledAt());
        assertEquals("https://meet.google.com/abc", session.getMeetingUrl());
        assertEquals(SessionStatuses.SCHEDULED, session.getStatus());
        verify(sessionRepository).save(session);
    }

    @Test
    void updateSession_ShouldThrowException_WhenScheduleOverlaps() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.existsOverlappingSession(any(), any(), any(), any())).thenReturn(true);

        UpdateSessionRequest req = new UpdateSessionRequest();
        req.setScheduledAt(Instant.now().plus(1, ChronoUnit.DAYS));

        AppException exception = assertThrows(AppException.class, () -> 
            bookingService.updateSession(bookingId, sessionId, mentorId, req)
        );

        assertEquals(BookingErrorCode.INVALID_SCHEDULE_TIME.getCode(), exception.getExceptionCode().getCode());
        assertEquals("Lịch học bị trùng với một buổi học khác của Mentor.", exception.getMessage());
    }

    @Test
    void updateSession_ShouldTransitionBookingToInProgress_WhenSessionStarts() {
        booking.setStatus(BookingStatuses.SCHEDULED);
        session.setStatus(SessionStatuses.SCHEDULED);
        
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        UpdateSessionRequest req = new UpdateSessionRequest();
        req.setStatus(SessionStatusTransitionPolicy.IN_PROGRESS);

        bookingService.updateSession(bookingId, sessionId, mentorId, req);

        assertEquals(SessionStatusTransitionPolicy.IN_PROGRESS, session.getStatus());
        assertNotNull(session.getActualStartedAt());
        assertEquals(BookingStatuses.IN_PROGRESS, booking.getStatus());
        
        verify(bookingRepository).save(booking);
        verify(sessionRepository).save(session);
    }

    @Test
    void updateSession_ShouldThrowException_WhenCompletedBeforeMinimumDuration() {
        session.setStatus(SessionStatusTransitionPolicy.IN_PROGRESS);
        session.setScheduledAt(Instant.now().plus(10, ChronoUnit.MINUTES)); // Scheduled in the future somehow
        
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        UpdateSessionRequest req = new UpdateSessionRequest();
        req.setStatus(SessionStatuses.COMPLETED);

        AppException exception = assertThrows(AppException.class, () -> 
            bookingService.updateSession(bookingId, sessionId, mentorId, req)
        );

        assertEquals(BookingErrorCode.INVALID_STATE_TRANSITION.getCode(), exception.getExceptionCode().getCode());
        assertTrue(exception.getMessage().contains("Không thể hoàn thành buổi học trước thời gian tối thiểu"));
    }

    @Test
    void updateSession_ShouldCompleteSessionAndBooking_WhenAllSessionsCompleted() {
        booking.setStatus(BookingStatuses.IN_PROGRESS);
        session.setStatus(SessionStatusTransitionPolicy.IN_PROGRESS);
        session.setScheduledAt(Instant.now().minus(1, ChronoUnit.HOURS));
        session.setActualStartedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.countUncompletedSessionsByBookingId(bookingId)).thenReturn(0L);

        UpdateSessionRequest req = new UpdateSessionRequest();
        req.setStatus(SessionStatuses.COMPLETED);

        bookingService.updateSession(bookingId, sessionId, mentorId, req);

        assertEquals(SessionStatuses.COMPLETED, session.getStatus());
        assertNotNull(session.getActualEndedAt());
        assertEquals(BookingStatuses.COMPLETED, booking.getStatus());
        
        verify(eventPublisher).publish(any(com.unishare.api.common.event.BookingCompletedEvent.class));
    }

    @Test
    void updateSession_ShouldPublishSessionCanceledEvent_WhenSessionIsCanceled() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        UpdateSessionRequest req = new UpdateSessionRequest();
        req.setStatus(SessionStatuses.CANCELED);
        req.setCancelReason("Bận đột xuất");

        bookingService.updateSession(bookingId, sessionId, mentorId, req);

        assertEquals(SessionStatuses.CANCELED, session.getStatus());
        assertEquals("Bận đột xuất", session.getCancelReason());
        assertEquals(mentorId, session.getCanceledBy());
        assertNotNull(session.getCanceledAt());
        
        verify(eventPublisher).publish(any(com.unishare.api.common.event.SessionCanceledEvent.class));
    }
}
