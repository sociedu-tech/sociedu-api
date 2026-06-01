package com.unishare.api.common.constants;

/** Khớp enum {@code session_status} trong schema. */
public final class SessionStatuses {

    private SessionStatuses() {}

    public static final String PENDING = "pending";
    public static final String SCHEDULED = "scheduled";
    public static final String COMPLETED = "completed";
    public static final String AWAITING_CONFIRMATION = "awaiting_confirmation";
    public static final String DISPUTED = "disputed";
    public static final String NO_SHOW = "no_show";
    public static final String CANCELED = "canceled";
}
