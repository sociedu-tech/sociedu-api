package com.unishare.api.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Upload multipart file lên storage provider.
     *
     * @param file   File cần upload (đã được caller validate kích thước/định dạng).
     * @param folder Tên thư mục lưu trữ trên provider (ví dụ: documents, avatars).
     * @return Thông tin vị trí lưu trữ: URL truy cập, publicId, resourceType.
     */
    StoredFileLocation uploadFile(MultipartFile file, String folder);

    /**
     * Xóa file đã upload bằng định danh chính xác do provider trả về.
     * Đây là cách xóa đáng tin cậy, ưu tiên dùng so với {@link #deleteFile(String)}.
     *
     * @param publicId     Định danh resource (Cloudinary: public_id; S3: object key…).
     * @param resourceType Loại resource (image|video|raw). Có thể null nếu provider không cần.
     * @return true nếu provider xác nhận đã xóa.
     */
    boolean deleteByPublicId(String publicId, String resourceType);

    /**
     * Xóa file bằng URL — chỉ dùng cho dữ liệu cũ không có publicId.
     * Có thể không đáng tin cậy nếu URL không suy ngược được sang publicId.
     */
    boolean deleteFile(String fileUrl);
}
