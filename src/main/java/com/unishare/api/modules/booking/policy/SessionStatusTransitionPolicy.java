package com.unishare.api.modules.booking.policy;

import com.unishare.api.common.constants.SessionStatuses;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class SessionStatusTransitionPolicy {

    // Adding "in_progress" to standard session statuses just for completeness and mapping
    public static final String IN_PROGRESS = "in_progress";

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            SessionStatuses.PENDING, Set.of(SessionStatuses.SCHEDULED, SessionStatuses.CANCELED),
            SessionStatuses.SCHEDULED, Set.of(
                    IN_PROGRESS,
                    SessionStatuses.AWAITING_CONFIRMATION,
                    SessionStatuses.COMPLETED,
                    SessionStatuses.DISPUTED,
                    SessionStatuses.CANCELED,
                    SessionStatuses.NO_SHOW),
            IN_PROGRESS, Set.of(
                    SessionStatuses.AWAITING_CONFIRMATION,
                    SessionStatuses.COMPLETED,
                    SessionStatuses.DISPUTED,
                    SessionStatuses.CANCELED,
                    SessionStatuses.NO_SHOW),
            SessionStatuses.AWAITING_CONFIRMATION, Set.of(
                    SessionStatuses.COMPLETED,
                    SessionStatuses.DISPUTED,
                    SessionStatuses.CANCELED),
            SessionStatuses.DISPUTED, Collections.emptySet(),
            SessionStatuses.COMPLETED, Collections.emptySet(),
            SessionStatuses.CANCELED, Collections.emptySet(),
            SessionStatuses.NO_SHOW, Collections.emptySet()
    );

    public static void validateTransition(String fromStatus, String toStatus) {
        if (fromStatus == null || toStatus == null) {
            throw new IllegalArgumentException("Session status cannot be null");
        }
        if (fromStatus.equals(toStatus)) {
            return; // No-op, allowed by idempotency
        }
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(fromStatus, Collections.emptySet());
        if (!allowed.contains(toStatus)) {
            throw new IllegalStateException("Invalid session status transition from " + fromStatus + " to " + toStatus);
        }
    }
}
