package com.unishare.api.modules.booking.service.impl;

import com.unishare.api.common.constants.BookingStatuses;
import com.unishare.api.common.constants.SessionStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.infrastructure.googlemeet.GoogleMeetService;
import com.unishare.api.modules.auth.repository.UserRepository;
import com.unishare.api.modules.booking.dto.ConfirmSessionCompletionRequest;
import com.unishare.api.modules.booking.entity.Booking;
import com.unishare.api.modules.booking.entity.BookingSession;
import com.unishare.api.modules.booking.exception.BookingErrorCode;
import com.unishare.api.modules.booking.repository.BookingRepository;
import com.unishare.api.modules.booking.repository.BookingSessionEvidenceRepository;
import com.unishare.api.modules.booking.repository.BookingSessionRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplConfirmSessionTest {

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
    @Mock
    private GoogleMeetService googleMeetService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private UUID bookingId;
    private UUID sessionId;
    private UUID buyerId;
    private UUID mentorId;
    private Booking booking;
    private BookingSession session;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        mentorId = UUID.randomUUID();

        booking = new Booking();
        booking.setId(bookingId);
        booking.setBuyerId(buyerId);
        booking.setMentorId(mentorId);
        booking.setStatus(BookingStatuses.IN_PROGRESS);

        session = new BookingSession();
        session.setId(sessionId);
        session.setBookingId(bookingId);
        session.setStatus(SessionStatuses.SCHEDULED);
        session.setScheduledAt(Instant.now().minusSeconds(3600));
    }

    @Test
    void confirmSessionCompletion_bothAgree_completesSession() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(evidenceRepository.findByBookingSessionId(sessionId)).thenReturn(java.util.List.of());
        when(sessionRepository.countUncompletedSessionsByBookingId(bookingId)).thenReturn(0L);

        ConfirmSessionCompletionRequest req = new ConfirmSessionCompletionRequest();
        req.setCompleted(true);

        bookingService.confirmSessionCompletion(bookingId, sessionId, buyerId, req);
        bookingService.confirmSessionCompletion(bookingId, sessionId, mentorId, req);

        verify(sessionRepository, org.mockito.Mockito.atLeastOnce()).save(any(BookingSession.class));
        assertEquals(SessionStatuses.COMPLETED, session.getStatus());
        assertEquals(Boolean.TRUE, session.getMenteeCompletionAck());
        assertEquals(Boolean.TRUE, session.getMentorCompletionAck());
    }

    @Test
    void confirmSessionCompletion_beforeDeadline_throws() {
        session.setScheduledAt(Instant.now().plusSeconds(3600));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        ConfirmSessionCompletionRequest req = new ConfirmSessionCompletionRequest();
        req.setCompleted(true);

        AppException ex = assertThrows(AppException.class,
                () -> bookingService.confirmSessionCompletion(bookingId, sessionId, buyerId, req));
        assertEquals(BookingErrorCode.SESSION_CONFIRM_TOO_EARLY, ex.getExceptionCode());
    }

    @Test
    void confirmSessionCompletion_oneRejects_marksDisputed() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(evidenceRepository.findByBookingSessionId(sessionId)).thenReturn(java.util.List.of());

        ConfirmSessionCompletionRequest agree = new ConfirmSessionCompletionRequest();
        agree.setCompleted(true);
        ConfirmSessionCompletionRequest reject = new ConfirmSessionCompletionRequest();
        reject.setCompleted(false);

        bookingService.confirmSessionCompletion(bookingId, sessionId, buyerId, agree);
        bookingService.confirmSessionCompletion(bookingId, sessionId, mentorId, reject);

        assertEquals(SessionStatuses.DISPUTED, session.getStatus());
    }
}
