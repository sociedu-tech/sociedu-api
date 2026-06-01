package com.unishare.api.modules.booking.support;

import com.unishare.api.modules.booking.service.BookingService;
import com.unishare.api.modules.order.entity.Order;
import com.unishare.api.modules.order.repository.OrderRepository;
import com.unishare.api.common.constants.OrderStatuses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSyncRunner implements CommandLineRunner {

    private final OrderRepository orderRepository;
    private final BookingService bookingService;

    @Override
    public void run(String... args) {
        log.info("[BookingSync] Scanning for paid orders with missing bookings...");
        try {
            List<Order> paidOrders = orderRepository.findByStatus(OrderStatuses.PAID);
            log.info("[BookingSync] Found {} paid order(s) in total.", paidOrders.size());
            
            int count = 0;
            for (Order order : paidOrders) {
                try {
                    bookingService.ensureBookingForOrder(order.getId());
                    count++;
                } catch (Exception e) {
                    log.error("[BookingSync] Failed to ensure booking for orderId=" + order.getId(), e);
                }
            }
            log.info("[BookingSync] Finished scanning. Processed {} paid order(s).", count);
        } catch (Exception e) {
            log.error("[BookingSync] Failed to scan paid orders", e);
        }
    }
}
