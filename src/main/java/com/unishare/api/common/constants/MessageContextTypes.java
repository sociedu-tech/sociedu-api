package com.unishare.api.common.constants;

/** Ngữ cảnh gắn kèm tin nhắn (không tạo conversation riêng). */
public final class MessageContextTypes {

    private MessageContextTypes() {}

    public static final String GENERAL = "general";
    public static final String ORDER = "order";
    public static final String BOOKING = "booking";
    public static final String SESSION = "session";

    public static boolean isKnown(String type) {
        if (type == null || type.isBlank()) {
            return false;
        }
        String t = type.trim().toLowerCase();
        return GENERAL.equals(t) || ORDER.equals(t) || BOOKING.equals(t) || SESSION.equals(t);
    }
}
