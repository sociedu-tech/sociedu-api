# Tài liệu Triển khai P0 — Khắc phục Mismatch API Backend ↔ Mobile

> **Trạng thái**: ✅ Hoàn thành  
> **Ngày cập nhật**: 2026-05-25  
> **Mục tiêu**: Sửa tất cả các API path mismatch giữa Backend và Mobile App để mobile có thể tích hợp được ngay khi tắt mock.

---

## Tổng quan

| # | Hạng mục | Trạng thái | Mô tả |
|---|----------|:----------:|--------|
| 1 | Chat / Conversation path mismatch | ✅ | Hỗ trợ song song 2 prefix, thêm detail + phân trang |
| 2 | File upload path mismatch | ✅ | Hỗ trợ cả 2 path, thêm validation file |
| 3 | Report / Dispute path mismatch | ✅ | Tạo alias controller cho mobile |
| 4 | Progress report path mismatch | ✅ | Tạo controller mới thống nhất namespace |
| 5 | Mentor profile lifecycle | ✅ | Thêm `GET /me/profile` + `POST /me/profile/submit` |

---

## 1. Module Trò chuyện (Chat / Conversations)

### Vấn đề
- Backend cấu hình tại `/api/v1/chat/conversations/...`
- Mobile gọi tới `/api/v1/conversations/...`
- Thiếu endpoint lấy chi tiết cuộc hội thoại theo ID
- Thiếu phân trang cho danh sách tin nhắn

### Giải pháp
- Hỗ trợ **song song** cả 2 prefix `/api/v1/chat/conversations` và `/api/v1/conversations`
- Thêm endpoint lấy chi tiết cuộc hội thoại: `GET /{conversationId}`
- Thêm API tải tin nhắn **phân trang** (sắp xếp `createdAt DESC` — tin nhắn mới nhất trước)
- Giữ nguyên API cũ dạng `List` (đánh dấu `@Deprecated`) để tương thích ngược

### Danh sách API

| Method | Path | Auth | Mô tả |
|--------|------|:----:|--------|
| `POST` | `/api/v1/conversations` | 🔒 JWT | Tạo cuộc hội thoại |
| `POST` | `/api/v1/chat/conversations` | 🔒 JWT | _(Alias)_ Tạo cuộc hội thoại |
| `GET` | `/api/v1/conversations` | 🔒 JWT | Danh sách cuộc hội thoại của tôi |
| `GET` | `/api/v1/chat/conversations` | 🔒 JWT | _(Alias)_ Danh sách cuộc hội thoại |
| `GET` | `/api/v1/conversations/{conversationId}` | 🔒 JWT | Chi tiết cuộc hội thoại |
| `GET` | `/api/v1/chat/conversations/{conversationId}` | 🔒 JWT | _(Alias)_ Chi tiết cuộc hội thoại |
| `GET` | `/api/v1/conversations/{conversationId}/messages?page=&size=&sort=` | 🔒 JWT | Tin nhắn phân trang (mới, DESC) |
| `GET` | `/api/v1/chat/conversations/{conversationId}/messages` | 🔒 JWT | _(Deprecated)_ Tin nhắn dạng `List` |
| `POST` | `/api/v1/conversations/{conversationId}/messages` | 🔒 JWT | Gửi tin nhắn |
| `POST` | `/api/v1/chat/conversations/{conversationId}/messages` | 🔒 JWT | _(Alias)_ Gửi tin nhắn |

### Kiểm thử
- **ChatControllerTest**: Tạo, lấy chi tiết, tải tin nhắn phân trang (DESC), tương thích ngược API cũ.

---

## 2. Module Upload File

### Vấn đề
- Backend cấu hình `POST /api/v1/files`
- Mobile gọi `POST /api/v1/files/upload`

### Giải pháp
- Hỗ trợ **cả 2 đường dẫn**
- Bổ sung validation định dạng và dung lượng file

### Danh sách API

| Method | Path | Auth | Mô tả |
|--------|------|:----:|--------|
| `POST` | `/api/v1/files` | 🔒 JWT | Upload file (multipart/form-data) |
| `POST` | `/api/v1/files/upload` | 🔒 JWT | _(Alias)_ Upload file |

### Quy tắc Validation

| Quy tắc | Chi tiết | Mã lỗi |
|----------|---------|--------|
| Dung lượng tối đa | 10MB | `FILE_SIZE_LIMIT_EXCEEDED` (400) |
| Định dạng cho phép | `jpg, jpeg, png, gif, pdf, doc, docx, xls, xlsx, ppt, pptx, txt, zip, rar` | `INVALID_FILE_TYPE` (400) |

### Response mẫu

```json
{
  "data": {
    "id": "uuid",
    "url": "https://...",
    "fileName": "certificate.pdf",
    "contentType": "application/pdf",
    "size": 123456,
    "visibility": "private"
  }
}
```

---

## 3. Module Báo cáo & Tranh chấp (Trust)

### Vấn đề
- Backend gom report/dispute trong namespace `/api/v1/trust/...`
- Mobile gọi trực tiếp `/api/v1/reports/...` và `/api/v1/disputes/...`

### Giải pháp
- Giữ nguyên `TrustController` tại `/api/v1/trust` (domain chính thức)
- Tạo 2 **alias controller** kế thừa (đánh dấu `@Deprecated`):
  - `ReportAliasController` → `/api/v1/reports`
  - `DisputeAliasController` → `/api/v1/disputes`
- Các alias ủy quyền toàn bộ sang `TrustService`

### Danh sách API

| Method | Path chính thức | Alias (Deprecated) | Auth | Mô tả |
|--------|----------------|---------------------|:----:|--------|
| `POST` | `/api/v1/trust/reports` | `/api/v1/reports` | 🔒 JWT | Tạo báo cáo |
| `GET` | `/api/v1/trust/reports/me` | `/api/v1/reports/me` | 🔒 JWT | Danh sách báo cáo của tôi |
| `POST` | `/api/v1/trust/reports/{reportId}/evidences` | `/api/v1/reports/{reportId}/evidences` | 🔒 JWT | Gửi bằng chứng |
| `PUT` | `/api/v1/trust/reports/{reportId}/resolve` | `/api/v1/reports/{reportId}/resolve` | 🔒 JWT | Giải quyết báo cáo |
| `POST` | `/api/v1/trust/disputes` | `/api/v1/disputes` | 🔒 JWT | Tạo tranh chấp |
| `GET` | `/api/v1/trust/disputes/me` | `/api/v1/disputes/me` | 🔒 JWT | Danh sách tranh chấp của tôi |
| `PUT` | `/api/v1/trust/disputes/{disputeId}/resolve` | `/api/v1/disputes/{disputeId}/resolve` | 🔒 JWT | Giải quyết tranh chấp |

---

## 4. Báo cáo Tiến độ Học tập (Progress Reports)

### Vấn đề
- Backend tách endpoint theo role: `/api/v1/mentee/reports` và `/api/v1/mentors/me/reports`
- Mobile gọi `/api/v1/progress-reports/...`

### Giải pháp
- Tạo `ProgressReportController` mới tại `/api/v1/progress-reports`
- Tự động xử lý trường hợp tài khoản có cả 2 vai trò (mentor + mentee)

### Danh sách API

| Method | Path | Auth | Mô tả |
|--------|------|:----:|--------|
| `POST` | `/api/v1/progress-reports` | 🔒 JWT (Mentee) | Nộp báo cáo tiến độ |
| `GET` | `/api/v1/progress-reports/me?role=mentee\|mentor` | 🔒 JWT | Danh sách báo cáo (xem chi tiết bên dưới) |
| `GET` | `/api/v1/progress-reports/{id}` | 🔒 JWT | Chi tiết báo cáo (yêu cầu quyền sở hữu) |
| `POST` | `/api/v1/progress-reports/{id}/mentor-feedback` | 🔒 JWT (Mentor) | Mentor phản hồi (hỗ trợ cả `PUT`) |

### Quy tắc đặc biệt — `GET /me`

| Điều kiện | Kết quả |
|-----------|---------|
| User là Mentee (hoặc `?role=mentee`) | Trả về danh sách báo cáo **đã gửi** |
| User là Mentor (hoặc `?role=mentor`) | Trả về danh sách báo cáo **được giao** |
| User có cả 2 role, không truyền `role` | Mặc định trả về Mentee view |

### Quy tắc Validation — Mentor feedback

| Quy tắc | Mã lỗi |
|----------|--------|
| Chỉ cho phép feedback khi báo cáo ở trạng thái `PENDING` | `PROGRESS_REPORT_INVALID_STATE` (400) |

### Kiểm thử
- **ProgressReportControllerTest**: Nộp báo cáo, xem chi tiết, danh sách động theo vai trò, phản hồi.
- **ProgressReportServiceImplTest**: Phân quyền xem chi tiết, kiểm tra trạng thái trước feedback.

---

## 5. Vòng đời Hồ sơ Mentor (Mentor Profile Submission)

### Vấn đề
- Backend có `GET /api/v1/mentors/me` và `PUT /api/v1/mentors/me`
- Mobile cần thêm `/api/v1/mentors/me/profile` và `/api/v1/mentors/me/profile/submit`

### Giải pháp
- Thêm 2 endpoint mới vào MentorController

### Danh sách API

| Method | Path | Auth | Mô tả |
|--------|------|:----:|--------|
| `GET` | `/api/v1/mentors/me/profile` | 🔒 JWT (Mentor) | Lấy thông tin profile mentor cá nhân |
| `POST` | `/api/v1/mentors/me/profile/submit` | 🔒 JWT (Mentor) | Gửi hồ sơ mentor để duyệt |

### Quy tắc Validation — Submit Profile

| Quy tắc | Mô tả | Mã lỗi |
|----------|--------|--------|
| State Machine | Nếu trạng thái đã `verified` → từ chối | `PROFILE_ALREADY_VERIFIED` (400) |
| Completeness | `headline`, `expertise`, `basePrice` không được trống | `PROFILE_INCOMPLETE` (400) |
| Idempotency | Nếu trạng thái đã `pending` → trả về thành công ngay, không ghi đè DB | — |

### Kiểm thử
- **MentorProfileControllerTest**: Xem profile, gửi duyệt thành công, chặn gửi khi thiếu thông tin (`PROFILE_INCOMPLETE`), chặn khi đã duyệt (`PROFILE_ALREADY_VERIFIED`).
