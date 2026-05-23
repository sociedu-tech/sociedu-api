# Module: File — Quản lý File tập trung

Hạ tầng lưu trữ dùng chung cho toàn bộ hệ thống — mọi upload/download đều đi qua module này.

## Entities
- `files` — metadata của file (uploader_id, file_name, file_url, mime_type, file_size, storage_provider, visibility, entity_type, entity_id, deleted_at)
- `visibility` — `public` | `private` | `internal`
- `entity_type` — tag xác định ngữ cảnh: `USER_AVATAR`, `USER_CERT`, `BOOKING_EVIDENCE`, `MESSAGE_ATTACHMENT`, `REPORT_EVIDENCE`

## Core Flows

**Upload file**:
1. Validate ở tầng service: file không rỗng, kích thước ≤ `FileUploadConstraints.MAX_FILE_SIZE_BYTES` (25MB), `Content-Type` thuộc whitelist (`ALLOWED_MIME_TYPES`).
2. Chuẩn hóa input: `folder` phải khớp `FOLDER_PATTERN` (fallback `uploads`), `visibility` chuẩn hóa về `public` | `private`, `fileName` strip path & cắt 255 ký tự.
3. Upload lên storage TRƯỚC (ngoài transaction DB) → nhận `StoredFileLocation(url, publicId, resourceType)`.
4. Lưu metadata trong transaction riêng qua `FileMetadataWriter` (Spring AOP proxy thực thụ).
5. Nếu DB lỗi → compensate: gọi `storage.deleteByPublicId` để tránh orphan file trên provider.

**Truy cập file**: GET với `fileId` → repo `findByIdAndDeletedAtIsNull` → nếu không `public` thì yêu cầu đúng `uploader_id` → trả `FileUploadResponse`.

**Xóa mềm**: Trong transaction, xác minh owner và set `deleted_at`. Sau khi commit, best-effort gọi `storage.deleteByPublicId(publicId, resourceType)` — log warn nếu provider trả false/ném exception, không làm hỏng transaction.

## Quan hệ Module
- **Được gọi bởi**: `user` (avatar, certificate), `booking` (evidence), `chat` (attachment), `report` (evidence).
- **Hoàn toàn không gọi module khác** — module này là leaf node trong dependency graph.

## Business Rules
- Chỉ `uploader_id` hoặc Admin mới được xóa file.
- File `private` chỉ có thể truy cập bởi `uploader_id` hoặc qua business context (ví dụ: cả hai bên của một booking).
- Validate `mime_type` whitelist và `file_size` tối đa trước khi lưu. Từ chối file không hợp lệ.
- `entity_type` + `entity_id` dùng để nhóm file theo ngữ cảnh nghiệp vụ — không dùng FK cứng.
