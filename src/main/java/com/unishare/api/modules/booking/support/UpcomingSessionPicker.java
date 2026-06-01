package com.unishare.api.modules.booking.support;

import com.unishare.api.modules.booking.entity.BookingSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class UpcomingSessionPicker {

    private UpcomingSessionPicker() {}

    public static Optional<BookingSession> pickNearest(List<BookingSession> sessions, Instant now) {
        if (sessions == null || sessions.isEmpty()) {
            return Optional.empty();
        }
        Optional<BookingSession> future = sessions.stream()
                .filter(s -> s.getScheduledAt() != null && !s.getScheduledAt().isBefore(now))
                .findFirst();
        return future.or(() -> Optional.of(sessions.get(0)));
    }
}
