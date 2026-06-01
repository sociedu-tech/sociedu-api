package com.unishare.api.modules.order.listener;

import com.unishare.api.common.constants.OrderStatuses;
import com.unishare.api.common.event.BookingCreatedEvent;
import com.unishare.api.common.event.OrderPaidEvent;
import com.unishare.api.common.event.PaymentProcessedEvent;
import com.unishare.api.common.event.PaymentSucceededEvent;
import com.unishare.api.infrastructure.event.DomainEventPublisher;
import com.unishare.api.modules.booking.repository.BookingRepository;
import com.unishare.api.modules.booking.service.BookingService;
import com.unishare.api.modules.order.service.OrderService;
import com.unishare.api.modules.service.service.CatalogReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sau khi cổng thanh toán commit giao dịch, cập nhật đơn rồi phát domain events
 * <strong>ngoài transaction</strong> (giống checkout HTTP) để notification listener nhận được.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessedEventListener {

    private final OrderService orderService;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final CatalogReadService catalogReadService;
    private final DomainEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        log.info("Payment processed orderId={} success={}", event.orderId(), event.success());
        boolean newlyPaid = orderService.applyPaymentResult(event.orderId(), event.success());
        if (!event.success()) {
            return;
        }

        var snap = orderService.getOrderSnapshot(event.orderId());
        log.info(
                "Payment reconcile orderId={} newlyPaid={} dbStatus={}",
                event.orderId(),
                newlyPaid,
                snap.status());
        if (!OrderStatuses.PAID.equals(snap.status())) {
            log.error("Payment success but orderId={} still status={} after applyPaymentResult", event.orderId(), snap.status());
            return;
        }

        if (newlyPaid) {
            var purchaseCtx = catalogReadService.resolvePurchaseContext(snap.serviceId());
            eventPublisher.publish(new OrderPaidEvent(event.orderId(), snap.buyerId(), purchaseCtx.mentorId()));
            log.info("Published OrderPaidEvent orderId={}", event.orderId());
        }

        try {
            if (bookingService.ensureBookingForOrder(event.orderId())) {
                bookingRepository.findByOrderId(event.orderId()).ifPresent(booking -> {
                    eventPublisher.publish(new BookingCreatedEvent(
                            booking.getId(),
                            event.orderId(),
                            booking.getBuyerId(),
                            booking.getMentorId()));
                    log.info("Published BookingCreatedEvent bookingId={} orderId={}", booking.getId(), event.orderId());
                });
            } else {
                log.info("ensureBookingForOrder did not create booking for orderId={}", event.orderId());
            }
        } catch (Exception e) {
            log.error("ensureBookingForOrder failed for orderId={}", event.orderId(), e);
        }

        eventPublisher.publish(new PaymentSucceededEvent(
                event.orderId(),
                snap.buyerId(),
                event.provider(),
                event.providerTransactionId(),
                event.paymentTransactionId()));
    }
}
