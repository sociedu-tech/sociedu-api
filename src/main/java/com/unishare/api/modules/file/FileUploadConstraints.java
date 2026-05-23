package com.unishare.api.modules.file;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Ràng buộc dùng chung khi upload file: whitelist MIME, kích thước, định dạng folder.
 *
 * <p>Hard-limit tại tầng service nên bằng/nhỏ hơn {@code spring.servlet.multipart.max-file-size}
 * (đang là 25MB). Khi servlet đã chặn trước thì service không bị gọi tới.</p>
 */
public final class FileUploadConstraints {

    private FileUploadConstraints() {}

    /** Kích thước tối đa cho 1 file (byte). Trùng / nhỏ hơn cấu hình multipart. */
    public static final long MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024;

    /** Folder chỉ cho phép ký tự an toàn, độ dài 1..64. */
    public static final Pattern FOLDER_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-/]{1,64}$");

    /** Folder mặc định nếu client không truyền. */
    public static final String DEFAULT_FOLDER = "uploads";

    /** Danh sách MIME được phép upload trong LMS. */
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            // Ảnh
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            // Tài liệu
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            // Nén
            "application/zip",
            "application/x-zip-compressed"
    );

    public static boolean isMimeAllowed(String mime) {
        return mime != null && ALLOWED_MIME_TYPES.contains(mime.toLowerCase());
    }
}
