package com.unishare.api.modules.user.dto;

/**
 * Phần tên từ profile user — tái sử dụng ở admin/auth; module consumer tự ghép vào response của họ.
 */
public record UserProfileNames(String firstName, String lastName) {

    public static final UserProfileNames EMPTY = new UserProfileNames(null, null);

    public String toDisplayName() {
        String a = firstName != null ? firstName.trim() : "";
        String b = lastName != null ? lastName.trim() : "";
        String joined = (a + " " + b).trim();
        return joined.isEmpty() ? null : joined;
    }
}
