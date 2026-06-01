package com.unishare.api.modules.booking.support;

import com.unishare.api.common.constants.SessionStatuses;
import com.unishare.api.modules.booking.entity.BookingSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpcomingSessionPickerTest {

    @Test
    void pickNearest_prefersSoonestFutureSession() {
        Instant now = Instant.parse("2026-06-01T10:00:00Z");
        BookingSession later = session(now.plus(3, ChronoUnit.DAYS));
        BookingSession sooner = session(now.plus(1, ChronoUnit.DAYS));

        Optional<BookingSession> picked =
                UpcomingSessionPicker.pickNearest(List.of(later, sooner), now);

        assertTrue(picked.isPresent());
        assertEquals(sooner.getId(), picked.get().getId());
    }

    @Test
    void pickNearest_fallsBackToEarliestWhenAllPast() {
        Instant now = Instant.parse("2026-06-01T10:00:00Z");
        BookingSession older = session(now.minus(3, ChronoUnit.DAYS));
        BookingSession newer = session(now.minus(1, ChronoUnit.DAYS));

        Optional<BookingSession> picked =
                UpcomingSessionPicker.pickNearest(List.of(older, newer), now);

        assertTrue(picked.isPresent());
        assertEquals(older.getId(), picked.get().getId());
    }

    private static BookingSession session(Instant scheduledAt) {
        BookingSession s = new BookingSession();
        s.setId(UUID.randomUUID());
        s.setBookingId(UUID.randomUUID());
        s.setCurriculumId(UUID.randomUUID());
        s.setTitle("Buổi test");
        s.setScheduledAt(scheduledAt);
        s.setStatus(SessionStatuses.SCHEDULED);
        return s;
    }
}
