package com.unishare.api.infrastructure.storage;

import lombok.Builder;
import lombok.Getter;

/**
 * Vị trí lưu trữ của một file đã upload thành công lên storage provider.
 * Giữ đủ thông tin để có thể xóa lại chính xác (Cloudinary cần publicId + resourceType).
 */
@Getter
@Builder
public class StoredFileLocation {

    /** URL truy cập file (thường là secure_url). */
    private final String url;

    /** Định danh đối tượng trên provider (Cloudinary: public_id; S3: object key…). */
    private final String publicId;

    /** Loại resource trên provider (image | video | raw …). Null nếu provider không phân loại. */
    private final String resourceType;
}
