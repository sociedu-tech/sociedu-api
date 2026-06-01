package com.unishare.api.modules.booking.service.impl;

import com.unishare.api.common.constants.BookingStatuses;
import com.unishare.api.common.constants.OrderStatuses;
import com.unishare.api.common.constants.SessionStatuses;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.common.dto.PageResponse;
import com.unishare.api.common.event.BookingCreatedEvent;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.modules.booking.dto.*;
import com.unishare.api.modules.booking.entity.Booking;
import com.unishare.api.modules.booking.entity.BookingSession;
import com.unishare.api.modules.booking.entity.BookingSessionEvidence;
import com.unishare.api.modules.booking.exception.BookingErrorCode;
import com.unishare.api.modules.booking.repository.BookingRepository;
import com.unishare.api.modules.booking.policy.SessionStatusTransitionPolicy;
import com.unishare.api.modules.booking.repository.BookingSessionEvidenceRepository;
import com.unishare.api.modules.booking.repository.BookingSessionRepository;
import com.unishare.api.modules.booking.service.BookingService;
import com.unishare.api.modules.booking.support.UpcomingSessionPicker;
import com.unishare.api.modules.user.dto.UserProfileNames;
import com.unishare.api.modules.user.service.UserService;
import com.unishare.api.modules.file.service.FileService;
import com.unishare.api.modules.order.dto.OrderSnapshot;
import com.unishare.api.modules.order.service.OrderService;
import com.unishare.api.modules.service.dto.PackageCurriculumSeedItem;
import com.unishare.api.modules.service.service.CatalogReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final List<String> UPCOMING_SESSION_STATUSES = List.of(
            SessionStatuses.PENDING,
            SessionStatuses.SCHEDULED,
            SessionStatusTransitionPolicy.IN_PROGRESS,
            SessionStatuses.AWAITING_CONFIRMATION);

    private final BookingRepository bookingRepository;
    private final BookingSessionRepository sessionRepository;
    private final BookingSessionEvidenceRepository evidenceRepository;
    private final OrderService orderService;
    private final CatalogReadService catalogReadService;
    private final FileService fileService;
    private final DomainEventPublisher eventPublisher;
    private final UserService userService;

    @Override
    @Transactional
    public void ensureBookingForOrder(UUID orderId) {
        OrderSnapshot snap = orderService.getOrderSnapshot(orderId);
        if (!OrderStatuses.PAID.equals(snap.status())) {
            return;
        }
        if (bookingRepository.findByOrderId(orderId).isPresent()) {
            return;
        }
        var ctx = catalogReadService.resolvePurchaseContext(snap.serviceId());
        Booking b = new Booking();
        b.setOrderId(orderId);
        b.setBuyerId(snap.buyerId());
        b.setMentorId(ctx.mentorId());
        b.setPackageId(ctx.packageId());
        b.setStatus(BookingStatuses.SCHEDULED);
        b = bookingRepository.save(b);
        seedSessions(b.getId(), snap.serviceId());
        eventPublisher.publish(new BookingCreatedEvent(b.getId(), orderId, b.getBuyerId(), b.getMentorId()));
    }

    private void seedSessions(UUID bookingId, UUID versionId) {
        List<PackageCurriculumSeedItem> rows = catalogReadService.listCurriculumForVersionOrdered(versionId);
        int i = 0;
        for (PackageCurriculumSeedItem c : rows) {
            BookingSession s = new BookingSession();
            s.setBookingId(bookingId);
            s.setCurriculumId(c.id());
            s.setTitle(c.title());
            s.setScheduledAt(Instant.now().plus(Duration.ofDays(i + 1L)));
            s.setStatus(SessionStatuses.SCHEDULED);
            sessionRepository.save(s);
            i++;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> listForBuyer(UUID buyerId, Pageable pageable) {
        return PageResponse.of(bookingRepository.findByBuyerId(buyerId, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> listForMentor(UUID mentorId, Pageable pageable) {
        return PageResponse.of(bookingRepository.findByMentorId(mentorId, pageable).map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public NextUpcomingSessionResponse getNextUpcomingSessionForBuyer(UUID buyerId) {
        List<BookingSession> candidates =
                sessionRepository.findUpcomingSessionsForBuyer(buyerId, UPCOMING_SESSION_STATUSES);
        return buildNextUpcomingSession(candidates, true);
    }

    @Override
    @Transactional(readOnly = true)
    public NextUpcomingSessionResponse getNextUpcomingSessionForMentor(UUID mentorId) {
        List<BookingSession> candidates =
                sessionRepository.findUpcomingSessionsForMentor(mentorId, UPCOMING_SESSION_STATUSES);
        return buildNextUpcomingSession(candidates, false);
    }

    private NextUpcomingSessionResponse buildNextUpcomingSession(
            List<BookingSession> candidates, boolean perspectiveIsBuyer) {
        Optional<BookingSession> picked = UpcomingSessionPicker.pickNearest(candidates, Instant.now());
        if (picked.isEmpty()) {
            return null;
        }
        BookingSession session = picked.get();
        Booking booking = bookingRepository.findById(session.getBookingId())
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
        UUID counterpartyId = perspectiveIsBuyer ? booking.getMentorId() : booking.getBuyerId();
        Map<UUID, UserProfileNames> names =
                userService.getProfileNamesByUserIds(List.of(counterpartyId));
        String counterpartyName = formatCounterpartyName(counterpartyId, names.get(counterpartyId));
        return NextUpcomingSessionResponse.builder()
                .bookingId(booking.getId())
                .sessionId(session.getId())
                .title(session.getTitle() != null && !session.getTitle().isBlank()
                        ? session.getTitle()
                        : "Buổi học")
                .scheduledAt(session.getScheduledAt())
                .status(session.getStatus())
                .counterpartyId(counterpartyId)
                .counterpartyName(counterpartyName)
                .build();
    }

    private static String formatCounterpartyName(UUID userId, UserProfileNames names) {
        if (names != null) {
            String display = names.toDisplayName();
            if (display != null && !display.isBlank()) {
                return display;
            }
        }
        String id = userId.toString().replace("-", "");
        String shortId = id.length() <= 8 ? id : id.substring(0, 8);
        return "Người dùng #" + shortId;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getById(UUID bookingId, UUID userId) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
        assertAccess(b, userId);
        return toResponse(b);
    }

    @Override
    @Transactional
    public BookingSessionResponse updateSession(UUID bookingId, UUID sessionId, UUID actorUserId, UpdateSessionRequest req) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));

        boolean isMentor = b.getMentorId().equals(actorUserId);
        boolean isBuyer = b.getBuyerId().equals(actorUserId);

        if (!isMentor && !isBuyer) {
            throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED);
        }

        BookingSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(BookingErrorCode.SESSION_NOT_FOUND));
        if (!s.getBookingId().equals(bookingId)) {
            throw new AppException(BookingErrorCode.SESSION_NOT_FOUND);
        }

        if (req.getScheduledAt() != null || req.getScheduledAtEnd() != null || req.getMeetingUrl() != null) {
            if (!isMentor) {
                throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED, "Chỉ Mentor mới được phép cập nhật lịch học và link meeting.");
            }
        }

        if (req.getScheduledAt() != null) {
            if (req.getScheduledAt().isBefore(Instant.now())) {
                throw new AppException(BookingErrorCode.INVALID_SCHEDULE_TIME, "Không thể xếp lịch trong quá khứ.");
            }
            // Simple overlap check (±1 hour buffer)
            Instant start = req.getScheduledAt().minus(Duration.ofHours(1));
            Instant end = req.getScheduledAt().plus(Duration.ofHours(1));
            if (sessionRepository.existsOverlappingSession(b.getMentorId(), s.getId(), start, end)) {
                throw new AppException(BookingErrorCode.INVALID_SCHEDULE_TIME, "Lịch học bị trùng với một buổi học khác của Mentor.");
            }
            s.setScheduledAt(req.getScheduledAt());
        }

        if (req.getScheduledAtEnd() != null) {
            if (s.getScheduledAt() != null && req.getScheduledAtEnd().isBefore(s.getScheduledAt())) {
                throw new AppException(BookingErrorCode.INVALID_SCHEDULE_TIME, "Thời gian kết thúc phải sau thời gian bắt đầu.");
            }
            s.setScheduledAtEnd(req.getScheduledAtEnd());
        }

        if (req.getMeetingUrl() != null) {
            s.setMeetingUrl(req.getMeetingUrl());
        }

        if (req.getStatus() != null && !s.getStatus().equals(req.getStatus())) {
            try {
                com.unishare.api.modules.booking.policy.SessionStatusTransitionPolicy.validateTransition(s.getStatus(), req.getStatus());
            } catch (IllegalStateException e) {
                throw new AppException(BookingErrorCode.INVALID_STATE_TRANSITION, e.getMessage());
            }
            
            if (SessionStatuses.COMPLETED.equals(req.getStatus())) {
                // Validation: now >= scheduledAt + minimumDuration (e.g. 15 mins)
                if (s.getScheduledAt() != null) {
                    Instant minCompletionTime = s.getScheduledAt().plus(Duration.ofMinutes(15));
                    if (Instant.now().isBefore(minCompletionTime)) {
                        throw new AppException(BookingErrorCode.INVALID_STATE_TRANSITION, "Không thể hoàn thành buổi học trước thời gian tối thiểu.");
                    }
                }
                if (s.getActualStartedAt() == null) {
                    throw new AppException(BookingErrorCode.INVALID_STATE_TRANSITION, "Phải bắt đầu buổi học (IN_PROGRESS) trước khi hoàn thành.");
                }
                
                s.setStatus(SessionStatuses.COMPLETED);
                s.setActualEndedAt(Instant.now());
                s.setCompletedAt(Instant.now());
                
            } else if (com.unishare.api.modules.booking.policy.SessionStatusTransitionPolicy.IN_PROGRESS.equals(req.getStatus())) {
                s.setStatus(req.getStatus());
                s.setActualStartedAt(Instant.now());
                
                if (BookingStatuses.PENDING.equals(b.getStatus()) || BookingStatuses.SCHEDULED.equals(b.getStatus())) {
                    com.unishare.api.modules.booking.policy.BookingStatusTransitionPolicy.validateTransition(b.getStatus(), BookingStatuses.IN_PROGRESS);
                    b.setStatus(BookingStatuses.IN_PROGRESS);
                    bookingRepository.save(b);
                }
            } else if (SessionStatuses.CANCELED.equals(req.getStatus())) {
                s.setStatus(req.getStatus());
                s.setCanceledBy(actorUserId);
                s.setCanceledAt(Instant.now());
                s.setCancelReason(req.getCancelReason() != null ? req.getCancelReason() : "Canceled by user");
                
                eventPublisher.publish(new com.unishare.api.common.event.SessionCanceledEvent(
                        b.getId(), s.getId(), actorUserId, s.getCancelReason()));
            } else if (SessionStatuses.NO_SHOW.equals(req.getStatus())) {
                s.setStatus(req.getStatus());
                // You can add logic for NO_SHOW here (who didn't show up, etc.)
                // Mentor could report mentee no-show, or mentee report mentor no-show
                // This could also trigger an event for dispute resolution
            } else {
                s.setStatus(req.getStatus());
            }
        }

        // Auto transition to SCHEDULED if both details are provided and it is still PENDING
        if (s.getScheduledAt() != null && s.getMeetingUrl() != null && SessionStatuses.PENDING.equals(s.getStatus())) {
            try {
                com.unishare.api.modules.booking.policy.SessionStatusTransitionPolicy.validateTransition(s.getStatus(), SessionStatuses.SCHEDULED);
                s.setStatus(SessionStatuses.SCHEDULED);
            } catch (IllegalStateException e) {
                throw new AppException(BookingErrorCode.INVALID_STATE_TRANSITION, e.getMessage());
            }
        }

        sessionRepository.save(s);
        
        // Aggregate completion check after saving session
        if (SessionStatuses.COMPLETED.equals(s.getStatus())) {
            checkAndCompleteBooking(b);
        }
        
        return mapSession(s);
    }

    private void checkAndCompleteBooking(Booking b) {
        long uncompletedCount = sessionRepository.countUncompletedSessionsByBookingId(b.getId());
        if (uncompletedCount == 0 && !BookingStatuses.COMPLETED.equals(b.getStatus())) {
            com.unishare.api.modules.booking.policy.BookingStatusTransitionPolicy.validateTransition(b.getStatus(), BookingStatuses.COMPLETED);
            b.setStatus(BookingStatuses.COMPLETED);
            bookingRepository.save(b);
            
            eventPublisher.publish(new com.unishare.api.common.event.BookingCompletedEvent(
                    b.getId(), b.getMentorId(), b.getBuyerId(), b.getOrderId()));
        }
    }

    @Override
    @Transactional
    public EvidenceResponse addEvidence(UUID bookingId, UUID sessionId, UUID userId, AddEvidenceRequest req) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
        if (!b.getBuyerId().equals(userId) && !b.getMentorId().equals(userId)) {
            throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED);
        }
        BookingSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(BookingErrorCode.SESSION_NOT_FOUND));
        if (!s.getBookingId().equals(bookingId)) {
            throw new AppException(BookingErrorCode.SESSION_NOT_FOUND);
        }
        fileService.getFile(req.getFileId(), userId);
        BookingSessionEvidence ev = new BookingSessionEvidence();
        ev.setBookingSessionId(sessionId);
        ev.setUploadedBy(userId);
        ev.setFileId(req.getFileId());
        ev.setDescription(req.getDescription());
        ev = evidenceRepository.save(ev);
        return EvidenceResponse.builder()
                .id(ev.getId())
                .uploadedBy(ev.getUploadedBy())
                .fileId(ev.getFileId())
                .description(ev.getDescription())
                .createdAt(ev.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID actorUserId, CancelBookingRequest req) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
        assertAccess(b, actorUserId);

        // Idempotent: if already canceled, return current state
        if (BookingStatuses.CANCELED.equals(b.getStatus())) {
            return toResponse(b);
        }

        // Validate transition
        if (!BookingStatuses.PENDING.equals(b.getStatus())
                && !BookingStatuses.SCHEDULED.equals(b.getStatus())
                && !BookingStatuses.IN_PROGRESS.equals(b.getStatus())) {
            throw new AppException(BookingErrorCode.BOOKING_CANNOT_CANCEL,
                    "Booking cannot be canceled in current state: " + b.getStatus());
        }

        // Cancel all sessions that are not completed, not in_progress, and not already canceled
        List<BookingSession> sessions = sessionRepository.findByBookingIdOrderByScheduledAtAsc(b.getId());
        for (BookingSession s : sessions) {
            if (SessionStatuses.COMPLETED.equals(s.getStatus())
                    || SessionStatuses.CANCELED.equals(s.getStatus())
                    || com.unishare.api.modules.booking.policy.SessionStatusTransitionPolicy.IN_PROGRESS.equals(s.getStatus())) {
                continue;
            }
            s.setStatus(SessionStatuses.CANCELED);
            s.setCanceledBy(actorUserId);
            s.setCanceledAt(Instant.now());
            s.setCancelReason(req.getReason());
            sessionRepository.save(s);
        }

        // Cancel booking
        b.setStatus(BookingStatuses.CANCELED);
        bookingRepository.save(b);

        eventPublisher.publish(new com.unishare.api.common.event.BookingCanceledEvent(
                b.getId(), b.getOrderId(), actorUserId, req.getReason()));

        return toResponse(b);
    }

    @Override
    @Transactional
    public BookingSessionResponse completeSession(UUID bookingId, UUID sessionId, UUID actorUserId) {
        ConfirmSessionCompletionRequest req = new ConfirmSessionCompletionRequest();
        req.setCompleted(true);
        return confirmSessionCompletion(bookingId, sessionId, actorUserId, req);
    }

    @Override
    @Transactional
    public BookingSessionResponse confirmSessionCompletion(
            UUID bookingId, UUID sessionId, UUID actorUserId, ConfirmSessionCompletionRequest req) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
        assertAccess(b, actorUserId);

        BookingSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(BookingErrorCode.SESSION_NOT_FOUND));
        if (!s.getBookingId().equals(bookingId)) {
            throw new AppException(BookingErrorCode.SESSION_NOT_FOUND);
        }

        if (SessionStatuses.COMPLETED.equals(s.getStatus())
                || SessionStatuses.CANCELED.equals(s.getStatus())) {
            throw new AppException(BookingErrorCode.SESSION_ALREADY_FINALIZED,
                    "Buổi học đã kết thúc, không thể xác nhận thêm.");
        }

        if (SessionStatuses.DISPUTED.equals(s.getStatus())) {
            throw new AppException(BookingErrorCode.SESSION_ALREADY_FINALIZED,
                    "Buổi học đang tranh chấp. Vui lòng dùng báo cáo kiểm duyệt.");
        }

        if (s.getScheduledAt() != null && Instant.now().isBefore(s.getScheduledAt())) {
            throw new AppException(BookingErrorCode.SESSION_CONFIRM_TOO_EARLY,
                    "Chưa đến thời hạn buổi học, chưa thể xác nhận.");
        }

        boolean isMentor = b.getMentorId().equals(actorUserId);
        boolean isBuyer = b.getBuyerId().equals(actorUserId);
        boolean completed = Boolean.TRUE.equals(req.getCompleted());

        if (isBuyer) {
            if (s.getMenteeCompletionAck() != null
                    && s.getMenteeCompletionAck().equals(completed)) {
                return mapSession(s);
            }
            s.setMenteeCompletionAck(completed);
            s.setMenteeAckAt(Instant.now());
        } else if (isMentor) {
            if (s.getMentorCompletionAck() != null
                    && s.getMentorCompletionAck().equals(completed)) {
                return mapSession(s);
            }
            s.setMentorCompletionAck(completed);
            s.setMentorAckAt(Instant.now());
        }

        applyCompletionAckOutcome(b, s, actorUserId);
        sessionRepository.save(s);
        return mapSession(s);
    }

    private void applyCompletionAckOutcome(Booking b, BookingSession s, UUID actorUserId) {
        Boolean menteeAck = s.getMenteeCompletionAck();
        Boolean mentorAck = s.getMentorCompletionAck();

        if (Boolean.FALSE.equals(menteeAck) || Boolean.FALSE.equals(mentorAck)) {
            transitionSessionStatus(s, SessionStatuses.DISPUTED);
            eventPublisher.publish(new com.unishare.api.common.event.SessionDisputedEvent(
                    b.getId(), s.getId(), b.getBuyerId(), b.getMentorId()));
            return;
        }

        if (Boolean.TRUE.equals(menteeAck) && Boolean.TRUE.equals(mentorAck)) {
            transitionSessionStatus(s, SessionStatuses.COMPLETED);
            s.setCompletedAt(Instant.now());
            checkAndCompleteBooking(b);
            return;
        }

        if (Boolean.TRUE.equals(menteeAck) || Boolean.TRUE.equals(mentorAck)) {
            if (!SessionStatuses.AWAITING_CONFIRMATION.equals(s.getStatus())) {
                transitionSessionStatus(s, SessionStatuses.AWAITING_CONFIRMATION);
            }
            eventPublisher.publish(new com.unishare.api.common.event.SessionAwaitingConfirmationEvent(
                    b.getId(), s.getId(), b.getBuyerId(), b.getMentorId(), actorUserId));
        }
    }

    private void transitionSessionStatus(BookingSession s, String targetStatus) {
        if (targetStatus.equals(s.getStatus())) {
            return;
        }
        try {
            com.unishare.api.modules.booking.policy.SessionStatusTransitionPolicy.validateTransition(
                    s.getStatus(), targetStatus);
        } catch (IllegalStateException e) {
            throw new AppException(BookingErrorCode.INVALID_STATE_TRANSITION, e.getMessage());
        }
        s.setStatus(targetStatus);
    }

    private void assertAccess(Booking b, UUID userId) {
        if (!b.getBuyerId().equals(userId) && !b.getMentorId().equals(userId)) {
            throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED);
        }
    }

    private BookingResponse toResponse(Booking b) {
        List<BookingSession> sessions = sessionRepository.findByBookingIdOrderByScheduledAtAsc(b.getId());
        return BookingResponse.builder()
                .id(b.getId())
                .orderId(b.getOrderId())
                .buyerId(b.getBuyerId())
                .mentorId(b.getMentorId())
                .packageId(b.getPackageId())
                .status(b.getStatus())
                .progressPercent(b.getProgressPercent())
                .createdAt(b.getCreatedAt())
                .sessions(sessions.stream().map(this::mapSession).collect(Collectors.toList()))
                .build();
    }

<<<<<<< Updated upstream
=======
    @Override
    @Transactional
    public BookingSessionResponse createSession(UUID bookingId, UUID mentorId, CreateSessionRequest req) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
        if (!b.getMentorId().equals(mentorId)) {
            throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED, "Chỉ Mentor của Booking mới được phép thêm buổi học.");
        }

        BookingSession s = new BookingSession();
        s.setBookingId(bookingId);
        s.setTitle(req.getTitle());
        s.setStatus(SessionStatuses.PENDING);
        s.setVersion(1L);
        s = sessionRepository.save(s);

        return mapSession(s);
    }

    @Override
    @Transactional
    public BookingResponse updateProgress(UUID bookingId, UUID mentorId, int progressPercent) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
        if (!b.getMentorId().equals(mentorId)) {
            throw new AppException(BookingErrorCode.BOOKING_ACCESS_DENIED, "Chỉ Mentor mới được phép cập nhật tiến trình.");
        }
        if (progressPercent < 0 || progressPercent > 100) {
            throw new AppException(BookingErrorCode.INVALID_SCHEDULE_TIME, "Tiến trình phải từ 0 đến 100.");
        }
        b.setProgressPercent(progressPercent);
        bookingRepository.save(b);
        return toResponse(b);
    }

>>>>>>> Stashed changes
    private BookingSessionResponse mapSession(BookingSession s) {
        List<BookingSessionEvidence> evs = evidenceRepository.findByBookingSessionId(s.getId());
        return BookingSessionResponse.builder()
                .id(s.getId())
                .curriculumId(s.getCurriculumId())
                .title(s.getTitle())
                .scheduledAt(s.getScheduledAt())
                .scheduledAtEnd(s.getScheduledAtEnd())
                .completedAt(s.getCompletedAt())
                .status(s.getStatus())
                .meetingUrl(s.getMeetingUrl())
                .menteeCompletionAck(s.getMenteeCompletionAck())
                .mentorCompletionAck(s.getMentorCompletionAck())
                .menteeAckAt(s.getMenteeAckAt())
                .mentorAckAt(s.getMentorAckAt())
                .evidences(evs.stream().map(e -> EvidenceResponse.builder()
                        .id(e.getId())
                        .uploadedBy(e.getUploadedBy())
                        .fileId(e.getFileId())
                        .description(e.getDescription())
                        .createdAt(e.getCreatedAt())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
