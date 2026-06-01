package com.unishare.api.infrastructure.mail.listener;

import com.unishare.api.common.event.BookingCreatedEvent;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.common.event.BookingCreatedNotificationMailEvent;
import com.unishare.api.common.event.MentorApprovedNotificationMailEvent;
import com.unishare.api.common.event.MentorRejectedNotificationMailEvent;
import com.unishare.api.common.event.MentorRequestApprovedEvent;
import com.unishare.api.common.event.MentorRequestRejectedEvent;
import com.unishare.api.common.event.OrderPaidEvent;
import com.unishare.api.common.event.OrderPaidNotificationMailEvent;
import com.unishare.api.common.event.OrderPaymentFailedEvent;
import com.unishare.api.common.event.OrderPaymentFailedNotificationMailEvent;
import com.unishare.api.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Nối domain events với hàng đợi gửi mail — không gọi SMTP từ Order/Booking/Payment service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainMailEventListener {

    private final UserRepository userRepository;
    private final DomainEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderPaid(OrderPaidEvent event) {
        userRepository.findById(event.buyerId())
                .map(u -> u.getEmail())
                .filter(e -> e != null && !e.isBlank())
                .ifPresentOrElse(
                        email -> eventPublisher.publish(new OrderPaidNotificationMailEvent(email, event.orderId())),
                        () -> log.warn("[Mail] OrderPaid: no email for buyerId={}", event.buyerId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderPaymentFailed(OrderPaymentFailedEvent event) {
        userRepository.findById(event.buyerId())
                .map(u -> u.getEmail())
                .filter(e -> e != null && !e.isBlank())
                .ifPresentOrElse(
                        email -> eventPublisher.publish(
                                new OrderPaymentFailedNotificationMailEvent(email, event.orderId())),
                        () -> log.warn("[Mail] PaymentFailed: no email for buyerId={}", event.buyerId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingCreated(BookingCreatedEvent event) {
        String buyerEmail = userRepository.findById(event.buyerId())
                .map(u -> u.getEmail())
                .filter(e -> e != null && !e.isBlank())
                .orElse(null);
        String mentorEmail = userRepository.findById(event.mentorId())
                .map(u -> u.getEmail())
                .filter(e -> e != null && !e.isBlank())
                .orElse(null);
        if (buyerEmail == null && mentorEmail == null) {
            log.warn("[Mail] BookingCreated: no emails for bookingId={}", event.bookingId());
            return;
        }
        eventPublisher.publish(
                new BookingCreatedNotificationMailEvent(buyerEmail, mentorEmail, event.bookingId(), event.orderId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMentorRequestApproved(MentorRequestApprovedEvent event) {
        userRepository.findById(event.userId())
                .map(u -> u.getEmail())
                .filter(e -> e != null && !e.isBlank())
                .ifPresentOrElse(
                        email -> eventPublisher.publish(
                                new MentorApprovedNotificationMailEvent(email, event.requestId())),
                        () -> log.warn("[Mail] MentorApproved: no email for userId={}", event.userId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMentorRequestRejected(MentorRequestRejectedEvent event) {
        userRepository.findById(event.userId())
                .map(u -> u.getEmail())
                .filter(e -> e != null && !e.isBlank())
                .ifPresentOrElse(
                        email -> eventPublisher.publish(
                                new MentorRejectedNotificationMailEvent(email, event.requestId(), event.reason())),
                        () -> log.warn("[Mail] MentorRejected: no email for userId={}", event.userId()));
    }
}
