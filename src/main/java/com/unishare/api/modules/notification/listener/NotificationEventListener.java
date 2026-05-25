package com.unishare.api.modules.notification.listener;

import com.unishare.api.common.event.BookingCanceledEvent;
import com.unishare.api.common.event.BookingCompletedEvent;
import com.unishare.api.common.event.OrderPaidEvent;
import com.unishare.api.modules.booking.entity.Booking;
import com.unishare.api.modules.booking.repository.BookingRepository;
import com.unishare.api.modules.notification.dto.NotificationResponse;
import com.unishare.api.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final BookingRepository bookingRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCompleted(BookingCompletedEvent event) {
        log.info("Handling BookingCompletedEvent for booking ID: {}", event.bookingId());
        
        try {
            // Notify buyer
            NotificationResponse buyerNotif = notificationService.createNotification(
                    event.buyerId(),
                    "Buổi học hoàn thành",
                    "Buổi học của bạn đã hoàn thành. Hãy gửi đánh giá cho mentor ngay nhé!",
                    "BOOKING",
                    "booking",
                    event.bookingId(),
                    Map.of(
                            "bookingId", event.bookingId().toString(),
                            "mentorId", event.mentorId().toString()
                    )
            );
            notificationService.sendPushNotificationAsync(buyerNotif.getId());

            // Notify mentor
            NotificationResponse mentorNotif = notificationService.createNotification(
                    event.mentorId(),
                    "Buổi học hoàn thành",
                    "Buổi học với học viên đã hoàn thành. Số dư khả dụng của bạn đã được cập nhật.",
                    "BOOKING",
                    "booking",
                    event.bookingId(),
                    Map.of(
                            "bookingId", event.bookingId().toString(),
                            "buyerId", event.buyerId().toString()
                    )
            );
            notificationService.sendPushNotificationAsync(mentorNotif.getId());
        } catch (Exception e) {
            log.error("Failed to process notifications for BookingCompletedEvent: {}", event.bookingId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingCanceled(BookingCanceledEvent event) {
        log.info("Handling BookingCanceledEvent for booking ID: {}", event.bookingId());
        
        try {
            Booking booking = bookingRepository.findById(event.bookingId()).orElse(null);
            if (booking == null) {
                log.warn("Booking not found when handling BookingCanceledEvent: {}", event.bookingId());
                return;
            }

            UUID recipientId;
            String title = "Lịch học đã bị hủy";
            String content;

            if (event.canceledBy().equals(booking.getBuyerId())) {
                // Buyer canceled -> Notify mentor
                recipientId = booking.getMentorId();
                content = "Học viên đã hủy lịch học. Lý do: " + event.cancelReason();
            } else {
                // Mentor canceled -> Notify buyer
                recipientId = booking.getBuyerId();
                content = "Mentor đã hủy lịch học. Lý do: " + event.cancelReason();
            }

            NotificationResponse notif = notificationService.createNotification(
                    recipientId,
                    title,
                    content,
                    "BOOKING",
                    "booking",
                    event.bookingId(),
                    Map.of(
                            "bookingId", event.bookingId().toString(),
                            "canceledBy", event.canceledBy().toString()
                    )
            );
            notificationService.sendPushNotificationAsync(notif.getId());
        } catch (Exception e) {
            log.error("Failed to process notifications for BookingCanceledEvent: {}", event.bookingId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaid(OrderPaidEvent event) {
        log.info("Handling OrderPaidEvent for order ID: {}", event.orderId());
        
        try {
            NotificationResponse notif = notificationService.createNotification(
                    event.buyerId(),
                    "Thanh toán thành công",
                    "Đơn hàng của bạn đã được thanh toán thành công.",
                    "ORDER",
                    "order",
                    event.orderId(),
                    Map.of("orderId", event.orderId().toString())
            );
            notificationService.sendPushNotificationAsync(notif.getId());
        } catch (Exception e) {
            log.error("Failed to process notifications for OrderPaidEvent: {}", event.orderId(), e);
        }
    }
}
