package com.unishare.api.common.constants;

/** Giá trị cột {@code files.visibility}. */
public final class FileVisibility {

    private FileVisibility() {}

    public static final String PRIVATE = "private";
    public static final String PUBLIC = "public";

    /**
     * Chuẩn hóa giá trị visibility do client gửi lên về {@link #PUBLIC} hoặc {@link #PRIVATE}.
     * Trả về {@link #PRIVATE} cho mọi giá trị null/không khớp — không hợp lệ thì coi như riêng tư.
     */
    public static String normalize(String raw) {
        if (raw != null && PUBLIC.equalsIgnoreCase(raw.trim())) {
            return PUBLIC;
        }
        return PRIVATE;
    }
}
