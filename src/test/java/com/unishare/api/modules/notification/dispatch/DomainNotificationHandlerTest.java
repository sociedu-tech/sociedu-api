package com.unishare.api.modules.notification.dispatch;

import com.unishare.api.common.event.BookingCreatedEvent;
import com.unishare.api.common.event.OrderCheckoutCreatedEvent;
import com.unishare.api.common.event.OrderPaidNotificationMailEvent;
import com.unishare.api.modules.booking.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class DomainNotificationHandlerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AdminRecipientResolver adminRecipientResolver;

    @InjectMocks
    private DomainNotificationHandler handler;

    @Test
    void supports_businessEvents() {
        assertTrue(handler.supports(new OrderCheckoutCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN)));
        assertFalse(handler.supports(new OrderPaidNotificationMailEvent("a@b.c", UUID.randomUUID())));
    }

    @Test
    void resolve_bookingCreated_notifiesBuyerAndMentor() {
        UUID bookingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID mentorId = UUID.randomUUID();
        var commands = handler.resolve(new BookingCreatedEvent(bookingId, UUID.randomUUID(), buyerId, mentorId));
        assertEquals(2, commands.size());
        assertTrue(commands.stream().anyMatch(c -> c.userId().equals(buyerId)));
        assertTrue(commands.stream().anyMatch(c -> c.userId().equals(mentorId)));
    }
}
