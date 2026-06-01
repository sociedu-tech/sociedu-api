package com.unishare.api.modules.notification.dispatch;

import com.unishare.api.common.event.*;
import com.unishare.api.modules.booking.entity.Booking;
import com.unishare.api.modules.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ánh xạ domain events nghiệp vụ → lệnh gửi notification (in-app + push).
 * Mail OTP / verify email không đi qua đây (listener mail riêng).
 */
@Component
@RequiredArgsConstructor
public class DomainNotificationHandler {

    private final BookingRepository bookingRepository;
    private final AdminRecipientResolver adminRecipientResolver;

    public boolean supports(DomainEvent event) {
        return event instanceof OrderCheckoutCreatedEvent
                || event instanceof OrderPaidEvent
                || event instanceof OrderPaymentFailedEvent
                || event instanceof BookingCreatedEvent
                || event instanceof BookingCompletedEvent
                || event instanceof BookingCanceledEvent
                || event instanceof SessionCanceledEvent
                || event instanceof SessionAwaitingConfirmationEvent
                || event instanceof SessionDisputedEvent
                || event instanceof MentorApplicationSubmittedEvent
                || event instanceof MentorRequestApprovedEvent
<<<<<<< Updated upstream
                || event instanceof MentorRequestRejectedEvent;
=======
                || event instanceof MentorRequestRejectedEvent
                || event instanceof SessionCompletedEvent
                || event instanceof SessionScheduledEvent
                || event instanceof ModerationReportCreatedEvent
                || event instanceof ModerationReportResolvedEvent
                || event instanceof BookingReviewCreatedEvent
                || event instanceof SessionReportRequestedEvent
                || event instanceof SessionReportSubmittedEvent;
>>>>>>> Stashed changes
    }

    public List<NotificationDispatchCommand> resolve(DomainEvent event) {
        if (event instanceof OrderCheckoutCreatedEvent e) {
            return onOrderCheckout(e);
        }
        if (event instanceof OrderPaidEvent e) {
            return onOrderPaid(e);
        }
        if (event instanceof OrderPaymentFailedEvent e) {
            return onOrderPaymentFailed(e);
        }
        if (event instanceof BookingCreatedEvent e) {
            return onBookingCreated(e);
        }
        if (event instanceof BookingCompletedEvent e) {
            return onBookingCompleted(e);
        }
        if (event instanceof BookingCanceledEvent e) {
            return onBookingCanceled(e);
        }
        if (event instanceof SessionCanceledEvent e) {
            return onSessionCanceled(e);
        }
        if (event instanceof SessionAwaitingConfirmationEvent e) {
            return onSessionAwaitingConfirmation(e);
        }
        if (event instanceof SessionDisputedEvent e) {
            return onSessionDisputed(e);
        }
        if (event instanceof MentorApplicationSubmittedEvent e) {
            return onMentorApplicationSubmitted(e);
        }
        if (event instanceof MentorRequestApprovedEvent e) {
            return onMentorApproved(e);
        }
        if (event instanceof MentorRequestRejectedEvent e) {
            return onMentorRejected(e);
        }
<<<<<<< Updated upstream
=======
        if (event instanceof SessionCompletedEvent e) {
            return onSessionCompleted(e);
        }
        if (event instanceof SessionScheduledEvent e) {
            return onSessionScheduled(e);
        }
        if (event instanceof ModerationReportCreatedEvent e) {
            return onModerationReportCreated(e);
        }
        if (event instanceof ModerationReportResolvedEvent e) {
            return onModerationReportResolved(e);
        }
        if (event instanceof BookingReviewCreatedEvent e) {
            return onBookingReviewCreated(e);
        }
        if (event instanceof SessionReportRequestedEvent e) {
            return onSessionReportRequested(e);
        }
        if (event instanceof SessionReportSubmittedEvent e) {
            return onSessionReportSubmitted(e);
        }
>>>>>>> Stashed changes
        return List.of();
    }

    private List<NotificationDispatchCommand> onOrderCheckout(OrderCheckoutCreatedEvent e) {
        var meta = Map.<String, Object>of(
                "orderId", e.orderId().toString(),
                "mentorId", e.mentorId().toString());
        return List.of(
                cmd(e.buyerId(), "Đơn hàng đã tạo", "Hoàn tất thanh toán để xác nhận đặt gói mentor.", "ORDER", "order", e.orderId(), meta),
                cmd(e.mentorId(), "Đơn mới chờ thanh toán", "Học viên vừa tạo đơn — sẽ thông báo lại khi thanh toán thành công.", "ORDER", "order", e.orderId(), meta));
    }

    private List<NotificationDispatchCommand> onOrderPaid(OrderPaidEvent e) {
        return List.of(cmd(
                e.buyerId(),
                "Thanh toán thành công",
                "Đơn hàng của bạn đã được thanh toán. Lịch học sẽ được tạo trong giây lát.",
                "ORDER",
                "order",
                e.orderId(),
                Map.of("orderId", e.orderId().toString())));
    }

    private List<NotificationDispatchCommand> onOrderPaymentFailed(OrderPaymentFailedEvent e) {
        return List.of(cmd(
                e.buyerId(),
                "Thanh toán chưa thành công",
                "Giao dịch không hoàn tất. Bạn có thể thử thanh toán lại từ đơn hàng.",
                "ORDER",
                "order",
                e.orderId(),
                Map.of("orderId", e.orderId().toString())));
    }

    private List<NotificationDispatchCommand> onBookingCreated(BookingCreatedEvent e) {
        var meta = Map.<String, Object>of(
                "bookingId", e.bookingId().toString(),
                "orderId", e.orderId().toString());
        return List.of(
                cmd(e.buyerId(), "Lịch học đã sẵn sàng", "Gói đã kích hoạt — xem buổi học và lịch trong mục Phiên học.", "BOOKING", "booking", e.bookingId(), meta),
                cmd(e.mentorId(), "Học viên mới", "Có booking mới từ thanh toán thành công. Kiểm tra lịch dạy và học viên.", "BOOKING", "booking", e.bookingId(), meta));
    }

    private List<NotificationDispatchCommand> onBookingCompleted(BookingCompletedEvent e) {
        var meta = Map.<String, Object>of(
                "bookingId", e.bookingId().toString(),
                "orderId", e.orderId().toString());
        return List.of(
                cmd(e.buyerId(), "Buổi học hoàn thành", "Khóa học đã hoàn tất. Hãy đánh giá mentor khi có thể.", "BOOKING", "booking", e.bookingId(), meta),
                cmd(e.mentorId(), "Buổi học hoàn thành", "Booking với học viên đã hoàn tất. Số dư có thể được cập nhật.", "BOOKING", "booking", e.bookingId(), meta));
    }

    private List<NotificationDispatchCommand> onBookingCanceled(BookingCanceledEvent e) {
        Booking booking = bookingRepository.findById(e.bookingId()).orElse(null);
        if (booking == null) {
            return List.of();
        }
        UUID recipient;
        String content;
        if (e.canceledBy().equals(booking.getBuyerId())) {
            recipient = booking.getMentorId();
            content = "Học viên đã hủy lịch học. Lý do: " + nullToEmpty(e.cancelReason());
        } else {
            recipient = booking.getBuyerId();
            content = "Mentor đã hủy lịch học. Lý do: " + nullToEmpty(e.cancelReason());
        }
        return List.of(cmd(
                recipient,
                "Lịch học đã bị hủy",
                content,
                "BOOKING",
                "booking",
                e.bookingId(),
                Map.of("bookingId", e.bookingId().toString(), "canceledBy", e.canceledBy().toString())));
    }

    private List<NotificationDispatchCommand> onSessionCanceled(SessionCanceledEvent e) {
        Booking booking = bookingRepository.findById(e.bookingId()).orElse(null);
        if (booking == null) {
            return List.of();
        }
        UUID recipient = e.canceledBy().equals(booking.getBuyerId())
                ? booking.getMentorId()
                : booking.getBuyerId();
        String content = "Một buổi học trong booking đã bị hủy. Lý do: " + nullToEmpty(e.cancelReason());
        return List.of(cmd(
                recipient,
                "Buổi học bị hủy",
                content,
                "BOOKING",
                "booking_session",
                e.sessionId(),
                Map.of("bookingId", e.bookingId().toString(), "sessionId", e.sessionId().toString())));
    }

    private List<NotificationDispatchCommand> onSessionAwaitingConfirmation(SessionAwaitingConfirmationEvent e) {
        UUID recipient = e.acknowledgedBy().equals(e.buyerId()) ? e.mentorId() : e.buyerId();
        return List.of(cmd(
                recipient,
                "Xác nhận buổi học",
                "Phía còn lại đã xác nhận buổi học. Vui lòng xác nhận trong mục Buổi học.",
                "BOOKING",
                "booking_session",
                e.sessionId(),
                Map.of("bookingId", e.bookingId().toString(), "sessionId", e.sessionId().toString())));
    }

    private List<NotificationDispatchCommand> onSessionDisputed(SessionDisputedEvent e) {
        var meta = Map.<String, Object>of(
                "bookingId", e.bookingId().toString(),
                "sessionId", e.sessionId().toString());
        return List.of(
                cmd(e.buyerId(), "Buổi học tranh chấp", "Hai bên chưa thống nhất hoàn thành buổi học. Có thể gửi báo cáo kiểm duyệt.", "BOOKING", "booking_session", e.sessionId(), meta),
                cmd(e.mentorId(), "Buổi học tranh chấp", "Hai bên chưa thống nhất hoàn thành buổi học. Có thể gửi báo cáo kiểm duyệt.", "BOOKING", "booking_session", e.sessionId(), meta));
    }

    private List<NotificationDispatchCommand> onMentorApplicationSubmitted(MentorApplicationSubmittedEvent e) {
        var meta = Map.<String, Object>of(
                "requestId", e.requestId().toString(),
                "userId", e.userId().toString());
        List<NotificationDispatchCommand> commands = new ArrayList<>();
        commands.add(cmd(
                e.userId(),
                "Đã gửi đơn mentor",
                "Đơn đăng ký của bạn đang chờ admin duyệt.",
                "MENTOR_APPLICATION",
                "mentor_application",
                e.requestId(),
                meta));
        for (UUID adminId : adminRecipientResolver.findAdminUserIds()) {
            if (adminId.equals(e.userId())) {
                continue;
            }
            commands.add(cmd(
                    adminId,
                    "Đơn đăng ký mentor mới",
                    "Có đơn đăng ký mentor chờ duyệt trong trang quản trị.",
                    "MENTOR_APPLICATION",
                    "mentor_application",
                    e.requestId(),
                    meta));
        }
        return commands;
    }

    private List<NotificationDispatchCommand> onMentorApproved(MentorRequestApprovedEvent e) {
        return List.of(cmd(
                e.userId(),
                "Trở thành mentor",
                "Đơn đăng ký mentor đã được duyệt. Bạn có thể tạo gói dịch vụ và nhận học viên.",
                "MENTOR_APPLICATION",
                "mentor_application",
                e.requestId(),
                Map.of("requestId", e.requestId().toString())));
    }

    private List<NotificationDispatchCommand> onMentorRejected(MentorRequestRejectedEvent e) {
        String reason = e.reason() != null && !e.reason().isBlank() ? e.reason() : "Không có lý do cụ thể";
        return List.of(cmd(
                e.userId(),
                "Đơn mentor bị từ chối",
                "Đơn đăng ký chưa được duyệt. Lý do: " + reason,
                "MENTOR_APPLICATION",
                "mentor_application",
                e.requestId(),
                Map.of("requestId", e.requestId().toString(), "reason", reason)));
    }

    private static NotificationDispatchCommand cmd(
            UUID userId,
            String title,
            String content,
            String type,
            String referenceType,
            UUID referenceId,
            Map<String, Object> metadata) {
        return new NotificationDispatchCommand(userId, title, content, type, referenceType, referenceId, metadata);
    }

<<<<<<< Updated upstream
=======
    private List<NotificationDispatchCommand> onModerationReportCreated(ModerationReportCreatedEvent e) {
        List<NotificationDispatchCommand> commands = new ArrayList<>();
        var meta = Map.<String, Object>of(
                "reportId", e.reportId().toString(),
                "type", e.type());

        // Notify reported user if any
        if (e.reportedUserId() != null) {
            commands.add(cmd(
                    e.reportedUserId(),
                    "Báo cáo vi phạm liên quan đến bạn",
                    "Hệ thống ghi nhận một báo cáo liên quan đến bạn. Lý do: " + e.reason(),
                    "MODERATION",
                    "moderation_report",
                    e.reportId(),
                    meta
            ));
        }

        // Notify Admins
        for (UUID adminId : adminRecipientResolver.findAdminUserIds()) {
            commands.add(cmd(
                    adminId,
                    "Yêu cầu kiểm duyệt mới",
                    "Có yêu cầu báo cáo vi phạm mới cần xử lý. Lý do: " + e.reason(),
                    "MODERATION",
                    "moderation_report",
                    e.reportId(),
                    meta
            ));
        }
        return commands;
    }

    private List<NotificationDispatchCommand> onModerationReportResolved(ModerationReportResolvedEvent e) {
        var meta = Map.<String, Object>of(
                "reportId", e.reportId().toString(),
                "status", e.status());
        String statusLabel = "resolved".equalsIgnoreCase(e.status()) ? "được chấp nhận" : "bị từ chối";
        return List.of(cmd(
                e.reporterId(),
                "Báo cáo đã xử lý",
                "Báo cáo vi phạm của bạn đã " + statusLabel + ". Ghi chú: " + nullToEmpty(e.resolutionNote()),
                "MODERATION",
                "moderation_report",
                e.reportId(),
                meta
        ));
    }

    private List<NotificationDispatchCommand> onBookingReviewCreated(BookingReviewCreatedEvent e) {
        var meta = Map.<String, Object>of(
                "bookingId", e.bookingId().toString(),
                "reviewId", e.reviewId().toString());
        return List.of(cmd(
                e.mentorId(),
                "Đánh giá mới từ học viên",
                "Học viên vừa gửi đánh giá " + e.rating() + " sao: " + nullToEmpty(e.comment()),
                "REVIEW",
                "booking_review",
                e.reviewId(),
                meta
        ));
    }

    private List<NotificationDispatchCommand> onSessionReportRequested(SessionReportRequestedEvent e) {
        var meta = Map.<String, Object>of(
                "requestId", e.requestId().toString(),
                "bookingId", e.bookingId().toString());
        return List.of(cmd(
                e.menteeId(),
                "Yêu cầu nộp báo cáo mới",
                "Mentor yêu cầu bạn nộp báo cáo: " + e.title(),
                "REPORT_REQUEST",
                "report_request",
                e.requestId(),
                meta));
    }

    private List<NotificationDispatchCommand> onSessionReportSubmitted(SessionReportSubmittedEvent e) {
        var meta = Map.<String, Object>of(
                "requestId", e.requestId().toString(),
                "bookingId", e.bookingId().toString());
        return List.of(cmd(
                e.mentorId(),
                "Học viên đã nộp báo cáo",
                "Học viên đã nộp báo cáo cho yêu cầu: " + e.title() + ". Vào mục chấm báo cáo để xem và duyệt.",
                "REPORT_REQUEST",
                "report_request",
                e.requestId(),
                meta));
    }

>>>>>>> Stashed changes
    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
